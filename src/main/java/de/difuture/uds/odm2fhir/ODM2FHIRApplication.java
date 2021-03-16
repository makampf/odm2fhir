package de.difuture.uds.odm2fhir;

/*
 * Copyright (C) 2021 DIFUTURE (https://difuture.de)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, copies
 * of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the
 * Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

import de.difuture.uds.odm2fhir.fhir.mapper.Subject;
import de.difuture.uds.odm2fhir.fhir.writer.FHIRBundleWriter;
import de.difuture.uds.odm2fhir.fhir.writer.FHIRBundler;
import de.difuture.uds.odm2fhir.odm.reader.ODMReader;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;

import java.nio.file.Path;

import static org.apache.commons.lang3.ArrayUtils.contains;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.function.Failable.asRunnable;

import static java.nio.file.Files.readString;

@Slf4j
@SpringBootApplication
public class ODM2FHIRApplication implements CommandLineRunner {

  @Autowired
  private ODMReader odmReader;

  @Autowired
  private FHIRBundler fhirBundler;

  @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
  @Autowired
  private FHIRBundleWriter fhirBundleWriter;

  @Value("classpath:README.md")
  private Path readme;

  @Value("classpath:odm/redcap/datadictionary.csv")
  private Path odmRedcapDatadictionary;

  @Value("${cron:}")
  private String cron;

  public static void main(String... args) {
    new SpringApplication(ODM2FHIRApplication.class).run(args);
  }

  @Override
  public void run(String... args) throws Exception {
    if (contains(args, "--help")) {
      log.info(readString(readme));
    } else if (contains(args, "--odm.redcap.datadictionary")) {
      log.info(readString(odmRedcapDatadictionary));
    } else {
      if (isBlank(cron)) {
        process();
      } else {
        new ConcurrentTaskScheduler().schedule(asRunnable(this::process), new CronTrigger(cron));
      }
    }
  }

  private void process() throws Exception {
    fhirBundleWriter.write(
        odmReader.read().getClinicalData().getSubjectData().stream()
            .map(subjectData -> new Subject().map(subjectData))
            .map(fhirBundler::bundle));
  }

}