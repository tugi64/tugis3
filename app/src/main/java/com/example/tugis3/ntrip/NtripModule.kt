package com.example.tugis3.ntrip

// Bu modül önce NtripClient için provider içeriyordu.
// AppModule zaten @Singleton NtripClient sağladığı için burada ikinci bir provider
// Hilt analizinde gereksiz / olası çakışmaya yol açıyordu. Bu nedenle içerik devre dışı bırakıldı.
// Gerekirse ileride NTRIP ile ilgili ek bağımlılıklar için yeniden düzenlenebilir.

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object NtripModule {
    // Intentionally left empty. (Single source of truth: AppModule.provideNtripClient())
}
