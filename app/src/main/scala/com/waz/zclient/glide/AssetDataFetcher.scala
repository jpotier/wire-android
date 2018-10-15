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
import com.waz.zclient.R

class AssetDataFetcher(context: Context) extends DataFetcher[InputStream] {
  override def loadData(priority: Priority, callback: DataFetcher.DataCallback[_ >: InputStream]): Unit = {
    val is = context.getResources.openRawResource(R.raw.test_image)
    callback.onDataReady(is)
  }

  override def cleanup(): Unit = {}

  override def cancel(): Unit = {}

  override def getDataClass: Class[InputStream] = classOf[InputStream]

  override def getDataSource: DataSource = DataSource.LOCAL
}
