/**
 * Wire
 * Copyright (C) 2018 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.waz.zclient.glide

import java.io.InputStream

import android.content.Context
import com.bumptech.glide.Priority
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.data.DataFetcher
import com.waz.ZLog
import com.waz.model.AssetId
import com.waz.service.assets.AssetService.BitmapResult.{BitmapLoaded, LoadingFailed}
import com.waz.ui.MemoryImageCache.BitmapRequest.Regular
import com.waz.zclient.common.views.ImageController
import com.waz.zclient.{Injectable, Injector}
import com.waz.ZLog.ImplicitTag.implicitLogTag
import com.waz.threading.Threading
import com.waz.utils.wrappers.Bitmap


class AssetDataFetcher(assetId: AssetId)(implicit context: Context, inj: Injector) extends DataFetcher[InputStream] with Injectable {

  //private lazy val zms = inject[Signal[ZMessaging]]
  private lazy val imageController = inject[ImageController]

  private lazy val bitmapSignal = imageController.imageSignal(assetId, Regular(300), forceDownload = true).collect {
    case BitmapLoaded(bm, _) => Option(bm)
    case LoadingFailed(_) => Option.empty[Bitmap]
  }.disableAutowiring()

  override def loadData(priority: Priority, callback: DataFetcher.DataCallback[_ >: InputStream]): Unit = {

    ZLog.verbose(s"loadData $assetId")

    bitmapSignal.head.foreach(_.foreach { bitmap =>

      ZLog.verbose(s"bitmapSignal  $assetId")

      import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
      import android.graphics.Bitmap.CompressFormat

      val bos = new ByteArrayOutputStream()
      bitmap.compress(CompressFormat.PNG, 0 , bos)
      val bitmapData = bos.toByteArray
      val is = new ByteArrayInputStream(bitmapData)
      callback.onDataReady(is)
    }) (Threading.Ui)
  }

  override def cleanup(): Unit = {}

  override def cancel(): Unit = {}

  override def getDataClass: Class[InputStream] = classOf[InputStream]

  override def getDataSource: DataSource = DataSource.REMOTE
}
