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
package com.waz.zclient.common.views

import android.graphics._
import com.waz.content.UserPreferences
import com.waz.model.AssetData.{IsImage, IsVideo}
import com.waz.model._
import com.waz.service.ZMessaging
import com.waz.service.assets.AssetService.BitmapResult
import com.waz.service.images.BitmapSignal
import com.waz.ui.MemoryImageCache.BitmapRequest
import com.waz.utils.events.Signal
import com.waz.utils.returning
import com.waz.utils.wrappers.URI
import com.waz.zclient.common.views.ImageAssetDrawable.ScaleType
import com.waz.zclient.common.views.ImageController._
import com.waz.zclient.{Injectable, Injector}

object ImageAssetDrawable {

  sealed trait ScaleType {
    def apply(matrix: Matrix, w: Int, h: Int, viewSize: Dim2): Unit
  }
  object ScaleType {
    case object FitXY extends ScaleType {
      override def apply(matrix: Matrix, w: Int, h: Int, viewSize: Dim2): Unit =
        matrix.setScale(viewSize.width.toFloat / w, viewSize.height.toFloat / h)
    }
    case object FitY extends ScaleType {
      override def apply(matrix: Matrix, w: Int, h: Int, viewSize: Dim2): Unit = {
        val scale = viewSize.height.toFloat / h
        matrix.setScale(scale, scale)
        matrix.postTranslate(- (w * scale - viewSize.width) / 2, 0)
      }
    }
    case object CenterCrop extends ScaleType {
      override def apply(matrix: Matrix, w: Int, h: Int, viewSize: Dim2): Unit = {
        val scale = math.max(viewSize.width.toFloat / w, viewSize.height.toFloat / h)
        val dx = - (w * scale - viewSize.width) / 2
        val dy = - (h * scale - viewSize.height) / 2

        matrix.setScale(scale, scale)
        matrix.postTranslate(dx, dy)
      }
    }
    case object CenterInside extends ScaleType {
      override def apply(matrix: Matrix, w: Int, h: Int, viewSize: Dim2): Unit = {
        val scale = math.min(viewSize.width.toFloat / w, viewSize.height.toFloat / h)
        val dx = - (w * scale - viewSize.width) / 2
        val dy = - (h * scale - viewSize.height) / 2

        matrix.setScale(scale, scale)
        matrix.postTranslate(dx, dy)
      }
    }
    case object CenterXCrop extends ScaleType {
      override def apply(matrix: Matrix, w: Int, h: Int, viewSize: Dim2): Unit = {
        val scale = math.max(viewSize.width.toFloat / w, viewSize.height.toFloat / h)
        val dx = - (w * scale - viewSize.width) / 2

        matrix.setScale(scale, scale)
        matrix.postTranslate(dx, 0)
      }
    }
  }
}

case class IntegrationSquareDrawHelper(scaleType: ScaleType ) {

  private val StrokeAlpha = 20
  private val padding = 0.1f

  private lazy val whitePaint = returning(new Paint(Paint.ANTI_ALIAS_FLAG)){ _.setColor(Color.WHITE) }
  private lazy val borderPaint = returning(new Paint(Paint.ANTI_ALIAS_FLAG)) { paint =>
    paint.setStyle(Paint.Style.STROKE)
    paint.setColor(Color.BLACK)
    paint.setAlpha(StrokeAlpha)
  }

  def cornerRadius(size: Float) = size * 0.2f
  def strokeWidth(size: Float) = size * 5f / 500f

  def draw(canvas: Canvas, bm: Bitmap, bounds: Rect, matrix: Matrix, bitmapPaint: Paint): Unit = {

    val strokeW = strokeWidth(bounds.width)

    borderPaint.setStrokeWidth(strokeW)
    val outerRect = new RectF(strokeW, strokeW, bounds.width - strokeW, bounds.height - strokeW)
    val backgroundRect = new RectF(strokeW, strokeW, bounds.width - strokeW, bounds.height - strokeW)
    val innerRect = new RectF(padding * bounds.width, padding * bounds.height, bounds.width - padding * bounds.width, bounds.height - padding * bounds.height)

    val matrix2 = new Matrix()
    scaleType(matrix2, bm.getWidth, bm.getHeight, Dim2(innerRect.width.toInt, innerRect.height.toInt))
    matrix2.postTranslate(innerRect.left, innerRect.top)

    val tempBm = Bitmap.createBitmap(bounds.width, bounds.height(), Bitmap.Config.ARGB_8888)
    val tempCanvas = new Canvas(tempBm)
    tempCanvas.drawBitmap(bm, matrix2, null)
    val shader = new BitmapShader(tempBm, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)

    val radius = cornerRadius(bounds.width)

    bitmapPaint.setShader(shader)
    canvas.drawRoundRect(backgroundRect, radius, radius, whitePaint)
    canvas.drawRoundRect(innerRect, 0, 0, bitmapPaint)
    canvas.drawRoundRect(outerRect, radius, radius, borderPaint)
  }
}

class ImageController(implicit inj: Injector) extends Injectable {

  val zMessaging = inject[Signal[ZMessaging]]

  def imageData(id: AssetId, zms: ZMessaging) =
    zms.assetsStorage.signal(id).flatMap {
      case a@IsImage() => Signal.const(a)
      case a@IsVideo() => a.previewId.fold(Signal.const(AssetData.Empty))(zms.assetsStorage.signal)
      case _ => Signal.const(AssetData.Empty)
    }

  def imageSignal(id: AssetId, req: BitmapRequest, forceDownload: Boolean): Signal[BitmapResult] =
    zMessaging.flatMap(imageSignal(_, id, req, forceDownload))

  def imageSignal(zms: ZMessaging, id: AssetId, req: BitmapRequest, forceDownload: Boolean = true): Signal[BitmapResult] =
    for {
      data <- imageData(id, zms)
      res <- BitmapSignal(data, req, zms.imageLoader, zms.network, zms.assetsStorage.get, zms.userPrefs.preference(UserPreferences.DownloadImagesAlways).signal, forceDownload)
    } yield res

  def imageSignal(uri: URI, req: BitmapRequest, forceDownload: Boolean): Signal[BitmapResult] =
    BitmapSignal(AssetData(source = Some(uri)), req, ZMessaging.currentGlobal.imageLoader, ZMessaging.currentGlobal.network, forceDownload = forceDownload)

  def imageSignal(data: AssetData, req: BitmapRequest, forceDownload: Boolean): Signal[BitmapResult] =
    zMessaging flatMap { zms => BitmapSignal(data, req, zms.imageLoader, zms.network, zms.assetsStorage.get, forceDownload = forceDownload) }

  def imageSignal(src: ImageSource, req: BitmapRequest, forceDownload: Boolean): Signal[BitmapResult] = src match {
    case WireImage(id) => imageSignal(id, req, forceDownload)
    case ImageUri(uri) => imageSignal(uri, req, forceDownload)
    case DataImage(data) => imageSignal(data, req, forceDownload)
    case NoImage() => Signal.empty[BitmapResult]
  }
}

object ImageController {

  sealed trait ImageSource
  case class WireImage(id: AssetId) extends ImageSource
  case class DataImage(data: AssetData) extends ImageSource
  case class ImageUri(uri: URI) extends ImageSource
  case class NoImage() extends ImageSource
}
