package org.apache.beam.sdk.io.gcp.healthcare;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.IOException;

@RunWith(JUnit4.class)
public class WebPathParserTest {

    @Test
    public void test_parsedAllElements() throws IOException {
        String webpathStr = "projects/foo/location/earth/datasets/bar/dicomStores/fee/dicomWeb/studies/abc/series/xyz/instances/123";

        WebPathParser parser = new WebPathParser();
        WebPathParser.DicomWebPath dicomWebPath = parser.parseDicomWebpath(webpathStr);

        Assert.assertNotNull(dicomWebPath);
        Assert.assertEquals("foo", dicomWebPath.project);
        Assert.assertEquals("earth", dicomWebPath.location);
        Assert.assertEquals("bar", dicomWebPath.dataset);
        Assert.assertEquals("fee", dicomWebPath.storeId);
        Assert.assertEquals("abc", dicomWebPath.studyId);
        Assert.assertEquals("xyz", dicomWebPath.seriesId);
        Assert.assertEquals("123", dicomWebPath.instanceId);
        Assert.assertEquals("projects/foo/location/earth/datasets/bar/dicomStores/fee", dicomWebPath.dicomStorePath);
    }
}
