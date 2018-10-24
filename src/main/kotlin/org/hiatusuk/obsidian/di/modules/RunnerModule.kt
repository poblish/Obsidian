package org.hiatusuk.obsidian.di.modules

import com.codahale.metrics.MetricRegistry
import com.codahale.metrics.httpclient.InstrumentedHttpClientConnectionManager
import com.google.common.eventbus.EventBus
import dagger.Module
import dagger.Provides
import de.odysseus.el.ExpressionFactoryImpl
import org.apache.http.client.HttpClient
import org.apache.http.impl.client.HttpClientBuilder
import org.hiatusuk.obsidian.di.ScenarioScope
import org.hiatusuk.obsidian.di.component.*
import org.hiatusuk.obsidian.di.includes.AssertLookupsComponent
import org.hiatusuk.obsidian.di.includes.CommandsComponent
import org.hiatusuk.obsidian.di.includes.ConfigsComponent
import org.hiatusuk.obsidian.di.includes.EventsComponent
import org.hiatusuk.obsidian.protocol.http.cookie.delegates.HttpCookiesState
import org.hiatusuk.obsidian.protocol.http.utils.TimedHttpRequestExecutor
import org.hiatusuk.obsidian.protocol.ssl.delegates.SslKeystores
import org.yaml.snakeyaml.Yaml
import javax.el.ExpressionFactory

@Module
class RunnerModule {

    @Provides
    @ScenarioScope
    internal fun assertLookups(component: RunnerComponent): AssertLookupsComponent {
        return component.newAssertLookupsComponent()
    }

    @Provides
    @ScenarioScope
    internal fun commands(component: RunnerComponent): CommandsComponent {
        return component.newCommandsComponent()
    }

    @Provides
    @ScenarioScope
    internal fun configs(component: RunnerComponent): ConfigsComponent {
        return component.newConfigsComponent()
    }

    @Provides
    @ScenarioScope
    internal fun events(component: RunnerComponent): EventsComponent {
        return component.newEventsComponent()
    }

    // This does actually belong in @ScenarioScope due to its dependencies
    @Provides
    @ScenarioScope
    internal fun provideSyncHttpClient(executor: TimedHttpRequestExecutor,
                                       ssl: SslKeystores,
                                       cookies: HttpCookiesState,
                                       inMetrics: MetricRegistry): HttpClient {
        val sslCtxt = requireNotNull(ssl).sslContext

        inMetrics.timer("Module.createHttpClient").time().use {
            return HttpClientBuilder.create().setRequestExecutor(requireNotNull(executor))
                    .setSSLContext(requireNotNull(ssl).sslContext)
                    .setDefaultCookieStore(cookies.cookieStore)
                    .setConnectionManager(InstrumentedHttpClientConnectionManager(inMetrics, ssl.createHttpRegistry(sslCtxt)))
                    .build()
        }
    }

    @Provides
    @ScenarioScope
    internal fun provideEventBus(): EventBus {
        return EventBus()
    }

    @Provides
    @ScenarioScope
    internal fun provideElExpressionFactory(): ExpressionFactory {
        return ExpressionFactoryImpl()
    }

    @Provides
    @ScenarioScope
    internal fun provideYaml(): Yaml {
        return Yaml()  // Be careful, is *not* threadsafe/reentrant!!
    }
}
