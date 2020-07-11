package com.newpage.salamport.media

import android.content.Context
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.media.session.MediaButtonReceiver


/**
 * Helper APIs for constructing MediaStyle notifications
 */
object MediaStyleHelper {
    /**
     * Build a notification using the information from the given media session. Makes heavy use
     * of [MediaMetadataCompat.getDescription] to extract the appropriate information.
     *
     * @param context      Context used to construct the notification.
     * @param mediaSession Media session to get information.
     * @return A pre-built notification with information from the given media session.
     */
    fun from(
        context: Context?, mediaSession: MediaSessionCompat
    ): NotificationCompat.Builder? {
        return try {
            val controller = mediaSession.controller
            val mediaMetadata = controller.metadata
            val description = mediaMetadata.description
            val builder =
                NotificationCompat.Builder(context)
            builder
                .setContentTitle(mediaMetadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE))
                .setContentText(mediaMetadata.getString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE))
                .setSubText(description.description)
                .setLargeIcon(description.iconBitmap)
                .setContentIntent(controller.sessionActivity)
                .setDeleteIntent(
                    MediaButtonReceiver.buildMediaButtonPendingIntent(
                        context,
                        PlaybackStateCompat.ACTION_STOP
                    )
                )
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            builder
        } catch (ex: NullPointerException) {
            Log.e("fa", "fu")
            null
        }
    }
}