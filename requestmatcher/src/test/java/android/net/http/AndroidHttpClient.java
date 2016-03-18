package android.net.http;

// Workaround for running Robolectric with API 23.
// Robolectric 3.1 will fix this.

// Basically this class has been removed so Robolectric
// must replace its implementation to avoid expecting it in the
// classpath
public class AndroidHttpClient {
}
