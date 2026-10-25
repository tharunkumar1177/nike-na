/*
 * Application entry: starts dependency injection and provides the Coil image loader with the
 * MediaStore artwork fetcher.
 */
package com.music.player

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.request.crossfade
import com.music.player.core.data.artwork.ArtworkFetcher
import com.music.player.core.data.artwork.ArtworkKeyer
import com.music.player.di.dataModule
import com.music.player.di.viewModelModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

/** Process-wide setup. */
class MusicApplication : Application(), SingletonImageLoader.Factory {

    /** Starts Koin before any component is created. */
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@MusicApplication)
            modules(dataModule, viewModelModule)
        }
    }

    /** Image loader that understands [com.music.player.core.model.Artwork] requests. */
    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .components {
                add(ArtworkFetcher.Factory(contentResolver))
                add(ArtworkKeyer())
            }
            .crossfade(true)
            .build()
}
