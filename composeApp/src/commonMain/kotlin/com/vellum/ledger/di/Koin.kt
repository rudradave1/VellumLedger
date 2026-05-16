package com.vellum.ledger.di

import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module
import com.vellum.ledger.data.createDataStore
import com.vellum.ledger.database.createLedgerDatabase
import com.vellum.ledger.sync.*
import com.vellum.ledger.repository.*
import com.vellum.ledger.domain.usecase.*
import com.vellum.ledger.ui.mapper.UiMapper
import com.vellum.ledger.ui.util.ExchangeRateProvider
import com.vellum.ledger.ui.provider.StringProvider
import com.vellum.ledger.ui.provider.CommonStringProvider
import com.vellum.ledger.ui.viewmodel.LedgerViewModel

val appModule = module {
    single { createDataStore() }
    single { createDefaultHttpClient() }
    single { DeviceIdentityManager(get()) }
    single { RemoteConfig(get()) }
    single<UserSession> { 
        SimpleUserSession(get())
    }
    single<LedgerApi> { KtorLedgerApi(get(), remoteConfig = get(), userSession = get()) }
    single { createLedgerDatabase() }
    single { ExchangeRateProvider(get()) }
    single<StringProvider> { CommonStringProvider() }
    
    single { SettingsRepository(get(), get(), get()) }
    single { TransactionRepository(get(), get()) }
    single { CardRepository(get()) }
    
    single { UiMapper(get(), get()) }
    
    single { GetAnalyticsUseCase(get(), get(), get()) }
    single { SyncTransactionsUseCase(get(), get()) }
    single { ExportTransactionsUseCase(get()) }
    
    factory { LedgerViewModel(get(), get(), get(), get(), get(), get(), get(), get()) }
}

fun initKoin(appDeclaration: KoinAppDeclaration = {}) =
    startKoin {
        appDeclaration()
        modules(appModule)
    }
