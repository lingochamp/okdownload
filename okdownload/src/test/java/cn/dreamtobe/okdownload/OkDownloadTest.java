package cn.dreamtobe.okdownload;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import cn.dreamtobe.okdownload.core.breakpoint.BreakpointStore;
import cn.dreamtobe.okdownload.core.breakpoint.BreakpointStoreOnCache;
import cn.dreamtobe.okdownload.core.breakpoint.DownloadStrategy;
import cn.dreamtobe.okdownload.core.connection.DownloadConnection;
import cn.dreamtobe.okdownload.core.connection.DownloadUrlConnection;
import cn.dreamtobe.okdownload.core.dispatcher.CallbackDispatcher;
import cn.dreamtobe.okdownload.core.dispatcher.DownloadDispatcher;
import cn.dreamtobe.okdownload.core.file.DefaultProcessFileStrategy;
import cn.dreamtobe.okdownload.core.file.DownloadOutputStream;
import cn.dreamtobe.okdownload.core.file.ProcessFileStrategy;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.robolectric.RuntimeEnvironment.application;
import static org.robolectric.annotation.Config.NONE;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = NONE)
public class OkDownloadTest {

    @Test(expected = IllegalArgumentException.class)
    public void setSingleInstance_onlyOnceValid() {
        OkDownload okDownload = mock(OkDownload.class);
        OkDownload.setSingletonInstance(okDownload);

        assertThat(OkDownload.with()).isEqualTo(okDownload);

        // second time
        OkDownload.setSingletonInstance(mock(OkDownload.class));
    }

    @Test
    public void create_NoParam() {
        OkDownload.Builder builder = new OkDownload.Builder(application);
        OkDownload okDownload = builder.build();

        assertThat(okDownload.downloadDispatcher()).isInstanceOf(DownloadDispatcher.class);
        assertThat(okDownload.callbackDispatcher()).isInstanceOf(CallbackDispatcher.class);
        assertThat(okDownload.breakpointStore()).isInstanceOf(BreakpointStoreOnCache.class);
        assertThat(okDownload.connectionFactory()).isInstanceOf(DownloadUrlConnection.Factory.class);
        assertThat(okDownload.outputStreamFactory()).isInstanceOf(DownloadOutputStream.Factory.class);
        assertThat(okDownload.processFileStrategy()).isInstanceOf(DefaultProcessFileStrategy.class);
        assertThat(okDownload.downloadStrategy()).isInstanceOf(DownloadStrategy.class);
    }

    @Test
    public void create_WithParam() {
        final DownloadDispatcher mockDownloadDispatcher = mock(DownloadDispatcher.class);
        final CallbackDispatcher mockCallbackDispatcher = mock(CallbackDispatcher.class);
        final BreakpointStore mockBreakpointStore = mock(BreakpointStore.class);
        final DownloadConnection.Factory mockConnectionFactory = mock(DownloadConnection.Factory.class);
        final DownloadOutputStream.Factory mockOutputFactory = mock(DownloadOutputStream.Factory.class);
        final ProcessFileStrategy mockProcessFileStrategy = mock(ProcessFileStrategy.class);
        final DownloadStrategy mockDownloadStrategy = mock(DownloadStrategy.class);
        final DownloadMonitor mockMonitor = mock(DownloadMonitor.class);

        OkDownload.Builder builder =
                new OkDownload.Builder(application)
                        .downloadDispatcher(mockDownloadDispatcher)
                        .callbackDispatcher(mockCallbackDispatcher)
                        .breakpointStore(mockBreakpointStore)
                        .connectionFactory(mockConnectionFactory)
                        .outputStreamFactory(mockOutputFactory)
                        .processFileStrategy(mockProcessFileStrategy)
                        .downloadStrategy(mockDownloadStrategy)
                        .monitor(mockMonitor);

        OkDownload okDownload = builder.build();

        assertThat(okDownload.downloadDispatcher()).isEqualTo(mockDownloadDispatcher);
        assertThat(okDownload.callbackDispatcher()).isEqualTo(mockCallbackDispatcher);
        assertThat(okDownload.breakpointStore()).isEqualTo(mockBreakpointStore);
        assertThat(okDownload.connectionFactory()).isEqualTo(mockConnectionFactory);
        assertThat(okDownload.outputStreamFactory()).isEqualTo(mockOutputFactory);
        assertThat(okDownload.processFileStrategy()).isEqualTo(mockProcessFileStrategy);
        assertThat(okDownload.downloadStrategy()).isEqualTo(mockDownloadStrategy);
        assertThat(okDownload.monitor).isEqualTo(mockMonitor);
    }


}