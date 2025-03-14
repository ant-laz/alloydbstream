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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.arrow.flatbuf.Null;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.coders.BigEndianIntegerCoder;
import org.apache.beam.sdk.coders.KvCoder;
import org.apache.beam.sdk.coders.StringUtf8Coder;
import org.apache.beam.sdk.options.Default;
import org.apache.beam.sdk.options.Description;
import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.transforms.Create;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import transforms.WriteToAlloyDB;

public class ADBPipeline {
  // ---------   LOGGER ----------------------------------------------------------------------------
  // https://cloud.google.com/dataflow/docs/guides/logging
  // Instantiate Logger
  private static final Logger LOG = LoggerFactory.getLogger(ADBPipeline.class);
  // ---------   COMMAND LINE OPTIONS --------------------------------------------------------------
  // For custom command line options
  public interface MyPipelineOptions extends PipelineOptions {
    @Description("AlloyDB Database --targetDatabase=")
    @Default.String("my-database")
    String getDatabase();
    void setDatabase(String value);
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
    PCollection<Integer> lengths = lines.apply(ParDo.of(new CalcLineLength()));

    // step 5 of X : execute the pipeline
    pipeline.run().waitUntilFinish();


  }

}
