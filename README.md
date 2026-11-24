# TV B
Android IPTV player starter project with M3U/M3U8 playlist loading, Media3 playback, and a foreground HLS recording service.

## Build
Open in Android Studio or run `gradle assembleDebug` with Gradle 8.9 and JDK 17.

## Current recording scope
The recorder currently saves clear, media-playlist HLS segments to an app-private external-files `Recordings` folder. It deliberately does not bypass DRM or encrypted streams. Master playlists must be replaced with a direct media-playlist URL. Some stream formats (for example fragmented MP4/CMAF) may need a later muxing enhancement.

## Default playlist
The app starts with the public IPTV-org index playlist. Availability and rights vary by channel and region. Only use streams you are authorized to access and record.
