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

import android.content.Context
import android.graphics.drawable.Drawable
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.waz.model.AssetId
import com.waz.zclient.GlideRequest
import com.waz.zclient.glide.transformations.{BackgroundColorTransformation, BlurTransformation, ScaleTransformation}

object BackgroundRequest {

  val ScaleValue = 1.4f

  def apply(assetId: AssetId)(implicit context: Context): GlideRequest[Drawable] =
    GlideDrawable(assetId).transforms(new CenterCrop(),
      new ScaleTransformation(ScaleValue),
      new BlurTransformation(),
      new BackgroundColorTransformation())
      .transition(DrawableTransitionOptions.withCrossFade())
}
