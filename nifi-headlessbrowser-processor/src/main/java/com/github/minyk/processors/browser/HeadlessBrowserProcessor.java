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

import com.machinepublishers.jbrowserdriver.JBrowserDriver;
import com.machinepublishers.jbrowserdriver.Settings;
import com.machinepublishers.jbrowserdriver.Timezone;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.nar.NarClassLoader;
import org.apache.nifi.processor.*;
import org.apache.nifi.annotation.behavior.ReadsAttribute;
import org.apache.nifi.annotation.behavior.ReadsAttributes;
import org.apache.nifi.annotation.behavior.WritesAttribute;
import org.apache.nifi.annotation.behavior.WritesAttributes;
import org.apache.nifi.annotation.lifecycle.OnScheduled;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.SeeAlso;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.processor.io.InputStreamCallback;
import org.apache.nifi.processor.io.OutputStreamCallback;
import org.apache.nifi.processor.util.StandardValidators;
import org.apache.nifi.stream.io.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

@Tags({"get", "html", "browser", "source", "input", "dom"})
@CapabilityDescription("Returns the page source in its current state, including any DOM updates that occurred after page load.")
@WritesAttributes({
                @WritesAttribute(attribute="URL", description="URL of the source"),
                @WritesAttribute(attribute = "Timezone", description = "Timezone when page loading.")
        })
public class HeadlessBrowserProcessor extends AbstractProcessor {

    public static final PropertyDescriptor HOST_OR_IP = new PropertyDescriptor
            .Builder().name("Host")
            .description("Host name or IP address to bind client.")
            .required(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .defaultValue("localhost")
            .build();

    public static final PropertyDescriptor IS_URL_PROVIDED = new PropertyDescriptor
            .Builder().name("Url Provided")
            .description("If true, read the page from URL property else read page url from the flowfile content.")
            .required(true)
            .addValidator(StandardValidators.BOOLEAN_VALIDATOR)
            .allowableValues("true", "false")
            .defaultValue("false")
            .build();

    public static final PropertyDescriptor PAGE_URL = new PropertyDescriptor
            .Builder().name("Page URL")
            .description("URL for page")
            .required(false)
            .addValidator(StandardValidators.URL_VALIDATOR)
            .defaultValue("")
            .build();

    public static final PropertyDescriptor TIMEZONE = new PropertyDescriptor
            .Builder().name("Timezone")
            .description("Timezone for browser")
            .required(true)
            .allowableValues(JBrowserSettingsValidators.getAllTimezone())
            .defaultValue(Timezone.ASIA_SEOUL.name())
            .build();

    public static final PropertyDescriptor PORT_RANGE = new PropertyDescriptor
            .Builder().name("Port Range")
            .description("A port range (range is inclusive and separated by a dash) -- e.g., 10000-10007")
            .required(true)
            .defaultValue("50001-59999")
            .addValidator(JBrowserSettingsValidators.PORT_RANGE_VALIDATOR)
            .build();

    public static final Relationship SUCCESS = new Relationship.Builder()
            .name("success")
            .description("Relationship for response 200.")
            .build();

    public static final Relationship FAILED = new Relationship.Builder()
            .name("failed")
            .description("Relationship for failed.")
            .build();

    private List<PropertyDescriptor> descriptors;

    private Set<Relationship> relationships;

    private JBrowserDriver driver;

    private final static String CLASSPATH = "java.class.path";
    private final static String ATTR_URL = "url";
    private final static String ATTR_TIMEZONE = "timezone";

    static {
        if(HeadlessBrowserProcessor.class.getClassLoader() instanceof URLClassLoader) {
            URL[] urls = ((URLClassLoader) HeadlessBrowserProcessor.class.getClassLoader()).getURLs();

            String cp = "";
            for (URL url : urls) {
                cp = cp + ":" + url.getFile();
            }
            System.setProperty(CLASSPATH, System.getProperty(CLASSPATH, "") + cp);
        }
    }

    @Override
    protected void init(final ProcessorInitializationContext context) {
        final List<PropertyDescriptor> descriptors = new ArrayList<>();
        descriptors.add(HOST_OR_IP);
        descriptors.add(IS_URL_PROVIDED);
        descriptors.add(PAGE_URL);
        descriptors.add(TIMEZONE);
        descriptors.add(PORT_RANGE);

        this.descriptors = Collections.unmodifiableList(descriptors);

        final Set<Relationship> relationships = new HashSet<>();
        relationships.add(SUCCESS);
        relationships.add(FAILED);
        this.relationships = Collections.unmodifiableSet(relationships);
    }

    @Override
    public Set<Relationship> getRelationships() {
        return this.relationships;
    }

    @Override
    public final List<PropertyDescriptor> getSupportedPropertyDescriptors() {
        return descriptors;
    }

    @OnScheduled
    public void onScheduled(final ProcessContext context) {
        Timezone tz = Timezone.byName(context.getProperty(TIMEZONE).getValue());
        String range = context.getProperty(PORT_RANGE).getValue();
        String host = context.getProperty(HOST_OR_IP).getValue();

        driver = new JBrowserDriver(Settings.builder().processes(range, host).timezone(tz).build());
    }

    @Override
    public void onTrigger(final ProcessContext context, final ProcessSession session) throws ProcessException {
        boolean url_provided = context.getProperty(IS_URL_PROVIDED).asBoolean();
        FlowFile flowFile = session.get();

        if ( flowFile == null && !url_provided ) {
            return;
        }

        if(url_provided) {
            String url = context.getProperty(PAGE_URL).getValue();
            driver.get(url);
        } else {
            final byte[] value =new byte[(int) flowFile.getSize()];
            session.read(flowFile, new InputStreamCallback() {
                @Override
                public void process(InputStream in) throws IOException {
                    StreamUtils.fillBuffer(in, value);

                }
            });

            String url = Arrays.toString(value);

            try {
                new URL(url);
            } catch (MalformedURLException e) {
                getLogger().error("Malformed URL: " + url);
                return;
            }
            driver.get(url);
        }

        if(driver.getStatusCode() == HttpURLConnection.HTTP_OK ) {
            if(url_provided) {
                flowFile = session.create();
            }

            FlowFile outputflowFile = session.append(flowFile, new OutputStreamCallback() {
                @Override
                public void process(OutputStream out) throws IOException {
                    out.write(driver.getPageSource().getBytes());
                }
            });

            Map<String, String> attrs = new HashMap<>();
            attrs.put(ATTR_URL, driver.getCurrentUrl());
            attrs.put(ATTR_TIMEZONE, context.getProperty(TIMEZONE).getValue());

            outputflowFile = session.putAllAttributes(outputflowFile, attrs);

            getLogger().info("Move result to success connection: " + attrs.get(ATTR_URL));
            session.transfer(outputflowFile, SUCCESS);

        } else if(driver.getStatusCode() == HttpURLConnection.HTTP_NOT_FOUND) {
            getLogger().warn("Page is not found: " + driver.getCurrentUrl());
            if(flowFile == null) {
                flowFile = session.create();
            }
            session.transfer(session.penalize(flowFile), FAILED);
        } else {
            getLogger().warn("Move failed URL to failed connection: " + driver.getCurrentUrl());
            if(flowFile == null) {
                flowFile = session.create();
            }
            session.transfer(session.penalize(flowFile), FAILED);
        }
    }
}
