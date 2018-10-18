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
package com.waz.zclient.glide.loaders

import java.io.InputStream

import android.content.Context
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.load.model.ModelLoader.LoadData
import com.bumptech.glide.signature.ObjectKey
import com.waz.ZLog
import com.waz.ZLog.ImplicitTag.implicitLogTag
import com.waz.zclient.Injector
import com.waz.zclient.glide.{AssetDataFetcher, AssetDataRequest, AssetIdRequest, AssetRequest}

//TODO: Keep this one only
class AssetRequestModelLoader(implicit context: Context, inj: Injector) extends ModelLoader[AssetRequest, InputStream] {
  override def buildLoadData(model: AssetRequest, width: Int, height: Int, options: Options): ModelLoader.LoadData[InputStream] = {
    val key = model match {
      case AssetDataRequest(data) => new ObjectKey(data.id.str)
      case AssetIdRequest(id) => new ObjectKey(id.str)
    }

    ZLog.verbose(s"model: $model ($width, $height) $options")
    new LoadData[InputStream](key, new AssetDataFetcher(model, width))
  }

  override def handles(model: AssetRequest): Boolean = true
}
