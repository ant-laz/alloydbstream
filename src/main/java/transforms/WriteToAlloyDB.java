package transforms;

import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.values.KV;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tonyzaro.pipeline.ADBPipeline;

// ---------   DoFn ------------------------------------------------------------------------------
public  class WriteToAlloyDB extends DoFn<KV<Integer, String>, String> {
  // ---------   LOGGER ----------------------------------------------------------------------------
  // https://cloud.google.com/dataflow/docs/guides/logging
  // Instantiate Logger
  private static final Logger LOG = LoggerFactory.getLogger(WriteToAlloyDB.class);

  @ProcessElement
  public void processElement(@Element KV<Integer, String> msg, OutputReceiver<String> out) {

    //https://beam.apache.org/releases/javadoc/current/org/apache/beam/sdk/io/jdbc/JdbcIO.html
    //    pipeline
    //        .apply(...)
    // .apply(JdbcIO.<KV<Integer, String>>write()
    //        .withDataSourceConfiguration(JdbcIO.DataSourceConfiguration.create(
    //                "com.mysql.jdbc.Driver", "jdbc:mysql://hostname:3306/mydb")
    //            .withUsername("username")
    //            .withPassword("password"))
    //        .withStatement("insert into Person values(?, ?)")
    //        .withPreparedStatementSetter(new JdbcIO.PreparedStatementSetter<KV<Integer, String>>() {
    //          public void setParameters(KV<Integer, String> element, PreparedStatement query)
    //              throws SQLException {
    //            query.setInt(1, element.getKey());
    //            query.setString(2, element.getValue());
    //          }
    //        })
    //    );
    out.output("success");
    //debug
    LOG.info(msg.getKey().toString());
    LOG.info(msg.getValue().toString());
  }
}