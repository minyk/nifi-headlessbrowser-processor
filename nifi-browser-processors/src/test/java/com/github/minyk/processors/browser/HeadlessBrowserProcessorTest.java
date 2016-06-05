/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.minyk.processors.browser;

import com.machinepublishers.jbrowserdriver.Timezone;
import org.apache.nifi.util.MockFlowFile;
import org.apache.nifi.util.TestRunner;
import org.apache.nifi.util.TestRunners;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;


public class HeadlessBrowserProcessorTest {

    private TestRunner testRunner;

    @Before
    public void init() {
        testRunner = TestRunners.newTestRunner(HeadlessBrowserProcessor.class);
        testRunner.setProperty(HeadlessBrowserProcessor.IS_URL_PROVIDED, "true");
        testRunner.setProperty(HeadlessBrowserProcessor.PORT_RANGE, "50001-59999");
        testRunner.setProperty(HeadlessBrowserProcessor.TIMEZONE, Timezone.ASIA_SEOUL.name());
    }

    @Test
    public void testProcessorFor200() {
        testRunner.setProperty(HeadlessBrowserProcessor.PAGE_URL, "https://www.google.co.kr");
        testRunner.run();
        List<MockFlowFile> result = testRunner.getFlowFilesForRelationship(HeadlessBrowserProcessor.SUCCESS);

        Assert.assertEquals(1, result.size());
        result.get(0).assertAttributeExists("url");
    }

    @Test
    public void testProcessorFor404() {
        testRunner.setProperty(HeadlessBrowserProcessor.PAGE_URL, "https://www.google.co.kr/should-not-found.html");
        testRunner.run();
        List<MockFlowFile> result = testRunner.getFlowFilesForRelationship(HeadlessBrowserProcessor.FAILED);

        Assert.assertEquals(1, result.size());
    }

    @Test
    public void testProcessorForJavascriptExecution() {
        testRunner.setProperty(HeadlessBrowserProcessor.PAGE_URL, "https://www.google.co.kr");
        //Click "I'm Feeling Lucky" Button.
        testRunner.setProperty(HeadlessBrowserProcessor.JAVASCRIPT, "document.getElementsByName('btnI')[0].click();");
        testRunner.run();
        List<MockFlowFile> result = testRunner.getFlowFilesForRelationship(HeadlessBrowserProcessor.SUCCESS);

        Assert.assertEquals(1, result.size());
    }

}
