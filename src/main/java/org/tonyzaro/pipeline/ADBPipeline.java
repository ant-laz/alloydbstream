//  Copyright 2025 Google LLC
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//  http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.tonyzaro.pipeline;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.PreparedStatement;
import java.util.Arrays;
import java.util.List;
import javax.sql.DataSource;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.coders.StringUtf8Coder;
import org.apache.beam.sdk.io.jdbc.JdbcIO;
import org.apache.beam.sdk.io.jdbc.JdbcIO.PreparedStatementSetter;
import org.apache.beam.sdk.options.Default;
import org.apache.beam.sdk.options.Description;
import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.transforms.Create;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.transforms.SerializableFunction;
import org.apache.beam.sdk.values.PCollection;
import com.google.cloud.alloydb.SocketFactory;

import org.checkerframework.checker.initialization.qual.Initialized;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.UnknownKeyFor;
// Import SLF4J packages.
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ADBPipeline {
  // ---------   LOGGER ----------------------------------------------------------------------------
  // https://cloud.google.com/dataflow/docs/guides/logging
  // Instantiate Logger
  private static final Logger LOG = LoggerFactory.getLogger(ADBPipeline.class);
  // ---------   COMMAND LINE OPTIONS --------------------------------------------------------------
  // For custom command line options
  public interface MyPipelineOptions extends PipelineOptions {

    //    @Description("GSECOPS_CUSTOMER_ID --secOpsCustomerID=")
    //     @Default.String("asdf")
    //     String getSecOpsCustomerID();
    //     void setSecOpsCustomerID(String value);
    @Description("AlloyDB Database --alloydbDatabase=")
    @Default.String("mydatabasename")
    String getAlloydbDatabase();
    void setAlloydbDatabase(String value);

    @Description("AlloyDB User --alloydbUser=")
    @Default.String("myusername")
    String getAlloydbUser();
    void setAlloydbUser(String value);

    @Description("AlloyDB Password --alloydbPassword=")
    @Default.String("mypassword")
    String getAlloydbPassword();
    void setAlloydbPassword(String value);

    @Description("AlloyDB Instance --alloydbInstance=")
    @Default.String("projects/prj/locations/europe-west2/clusters/clustername/instances/myinstance")
    String getAlloydbInstance();
    void setAlloydbInstance(String value);
  }

  // ---------   DoFn ------------------------------------------------------------------------------
  static class CalcLineLength extends DoFn<String, Integer> {

    @ProcessElement
    public void processElement(@Element String msg, OutputReceiver<Integer> out) {
      // Dummy DoFn to test setup of Dataflow

      // Simply return length of string in input PCollection
      out.output(msg.length());

      // And output input string to log so we can check everything is set up correct
      LOG.info(msg);
    }
  }

  // --- Data source for AlloyDBw with pooling setup to not exhaust DB with manhy conections -------
  // https://beam.apache.org/releases/javadoc/current/org/apache/beam/sdk/io/jdbc/JdbcIO.html
  private static class MyDataSourceProviderFn implements SerializableFunction<Void, DataSource> {

    private static transient DataSource dataSource;
    private String username;
    private String password;
    private String database;
    private String instance;

    public MyDataSourceProviderFn(String username, String password, String database, String instance) {
      this.username = username;
      this.password = password;
      this.database = database;
      this.instance = instance;
    }

    private static DataSource getDataSource(String username, String password, String database, String instance) {

      if (dataSource == null) {
        // if we already have a data source then return it
        // otherwise let's create one
        //https://cloud.google.com/alloydb/docs/connect-language-connectors
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(String.format("jdbc:postgresql:///%s", database));
        config.setUsername(username);
        config.setPassword(password);
        config.addDataSourceProperty("socketFactory", "com.google.cloud.alloydb.SocketFactory");
        config.addDataSourceProperty("alloydbInstanceName", instance);
        config.addDataSourceProperty("alloydbIpType", "PRIVATE"); //PSA
        config.setMaximumPoolSize(10);  //TODO: work out how many connections are needed in pool
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        dataSource = new HikariDataSource(config);
      }
      return dataSource;
    }

    @Override
    public synchronized DataSource apply(Void input) {
      return getDataSource(this.username, this.password, this.database, this.instance);
    }
  }

  public static void main(String[] args) {
    // step 1 of X : Initialize the pipeline options
    PipelineOptionsFactory.register(MyPipelineOptions.class);

    MyPipelineOptions myOptions = PipelineOptionsFactory
        .fromArgs(args)
        .withValidation()
        .as(MyPipelineOptions.class);

    // step 2 of X : create the main pipeline
    Pipeline pipeline = Pipeline.create(myOptions);

    // step 3 of X : create an in memory PCollection
    final List<String> LINES = Arrays.asList(
        "To be, or not to be: that is the question: ",
        "Whether 'tis nobler in the mind to suffer ",
        "The slings and arrows of outrageous fortune, ",
        "Or to take arms against a sea of troubles, ");
    PCollection<String> lines = pipeline.apply(Create.of(LINES)).setCoder(StringUtf8Coder.of());

    // step 4 of X : compute line length & output to logs
    PCollection<Integer> lengths = lines.apply("Calculate line length",
        ParDo.of(new CalcLineLength()));

    // step 5 of X : write lines to a table in AlloyDB
    lines.apply("write to alloydb",
        JdbcIO.<String>write()  //TODO : add command line args for these inputs
        .withDataSourceProviderFn(
            new MyDataSourceProviderFn(
                myOptions.getAlloydbUser(),
                myOptions.getAlloydbPassword(),
                myOptions.getAlloydbDatabase(),
                myOptions.getAlloydbInstance()))
            .withStatement("INSERT INTO messages (message) values (?)")
            .withPreparedStatementSetter(new PreparedStatementSetter<String>() {
              @Override
              public void setParameters(String element,
                  @UnknownKeyFor @NonNull @Initialized PreparedStatement preparedStatement)
                  throws @UnknownKeyFor@NonNull@Initialized Exception {
                preparedStatement.setString(1, element);
              }
            }));

    // step 6 of X : execute the pipeline
    pipeline.run().waitUntilFinish();


  }

}
