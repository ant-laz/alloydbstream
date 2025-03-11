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
import java.util.HashMap;
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

  public static void main(String[] args) {
    // step 1 of X : Initialize the pipeline options
    PipelineOptionsFactory.register(MyPipelineOptions.class);

    MyPipelineOptions myOptions = PipelineOptionsFactory
        .fromArgs(args)
        .withValidation()
        .as(MyPipelineOptions.class);

    // step 2 of X : create the main pipeline
    Pipeline pipeline = Pipeline.create(myOptions);

    Map<Integer, String> map = new HashMap<>();
    map.put(1, "ERROR");
    map.put(2, "SLEEP");

    PCollection<KV<Integer, String>> dummyrecords =
        pipeline.apply(
            Create.of(map).withCoder(KvCoder.of(BigEndianIntegerCoder.of(), StringUtf8Coder.of())));

    dummyrecords.apply("Write to AlloyDB", ParDo.of(new WriteToAlloyDB()));


  }

}
