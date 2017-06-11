package br.com.concretesolutions.requestmatcher;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

public class IOReaderTest {

    @Test
    public void canCorrectlyIdentifyMimeTypeFromPath() {
        assertThat(IOReader.mimeTypeFromExtension("file.json"), is("application/json"));
        assertThat(IOReader.mimeTypeFromExtension("file.xml"), is("text/xml"));
        assertThat(IOReader.mimeTypeFromExtension("file.html"), is("text/html"));
        assertThat(IOReader.mimeTypeFromExtension("file.htm"), is("text/html"));
        assertThat(IOReader.mimeTypeFromExtension("file.js"), is("text/javascript"));
        assertThat(IOReader.mimeTypeFromExtension("file.txt"), is("text/plain"));
        assertThat(IOReader.mimeTypeFromExtension("file.css"), is("text/css"));
        assertThat(IOReader.mimeTypeFromExtension("file.jpg"), is("image/jpeg"));
    }

    @Test
    public void returnNullWhenCantFindMatch() {
        assertThat(IOReader.mimeTypeFromExtension("file.zip"), is(nullValue()));
        assertThat(IOReader.mimeTypeFromExtension("file.abc"), is(nullValue()));
    }
}
