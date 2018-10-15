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
import com.bumptech.glide.load.model.{ModelLoader, ModelLoaderFactory, MultiModelLoaderFactory}
import com.waz.model.AssetId

class AssetModelLoaderFactory(context: Context) extends ModelLoaderFactory[AssetId, InputStream] {
  override def build(multiFactory: MultiModelLoaderFactory): ModelLoader[AssetId, InputStream] = {
    new AssetModelLoader(context)
  }

  override def teardown(): Unit = {}
}
