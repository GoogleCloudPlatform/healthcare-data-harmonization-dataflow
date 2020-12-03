// Copyright 2020 Google LLC.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package org.apache.beam.sdk.io.gcp.healthcare;

import java.io.IOException;

import org.apache.beam.sdk.io.gcp.healthcare.WebPathParser;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Testing the Healthcare API webpath parser.
 */
@RunWith(JUnit4.class)
public class WebPathParserTest {
    private WebPathParser webPathParser;

    @Before
    public void createWebParser() {
        webPathParser = new WebPathParser();
    }

    @Test
    public void test_parsedAllElements() throws IllegalArgumentException {
        String webpathStr = "projects/foo/location/earth/datasets/bar/dicomStores/fee/dicomWeb/studies/abc/series/xyz/instances/123";

        WebPathParser.DicomWebPath dicomWebPath = webPathParser.parseDicomWebpath(webpathStr);

        Assert.assertNotNull(dicomWebPath);
        Assert.assertEquals("foo", dicomWebPath.project);
        Assert.assertEquals("earth", dicomWebPath.location);
        Assert.assertEquals("bar", dicomWebPath.dataset);
        Assert.assertEquals("fee", dicomWebPath.storeId);
        Assert.assertEquals("abc", dicomWebPath.studyId);
        Assert.assertEquals("xyz", dicomWebPath.seriesId);
        Assert.assertEquals("123", dicomWebPath.instanceId);
        Assert.assertEquals("projects/foo/location/earth/datasets/bar/dicomStores/fee",
                dicomWebPath.dicomStorePath);
    }

    @Test
    public void test_nonDicomWebpath() {
        String webpathStr = "foo/notADicomStore/bar";

        try {
            webPathParser.parseDicomWebpath(webpathStr);
            throw new AssertionError("WebPathParser incorrectly did not throw error");
        } catch (IllegalArgumentException e) {
            //
        }
    }

    @Test
    public void test_webPathTooLong() {
        String webpathStr = "projects/foo/location/earth/mars/datasets/bar/bam/" +
                "dicomStores/fee/dicomWeb/studies/abc/efg/series/xyz/instances/123/";

        WebPathParser.DicomWebPath dicomWebPath = webPathParser.parseDicomWebpath(webpathStr);

        Assert.assertNotEquals("bar", dicomWebPath.dataset);
        Assert.assertNotEquals("xyz", dicomWebPath.seriesId);
    }
}
