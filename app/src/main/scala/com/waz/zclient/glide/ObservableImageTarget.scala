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

import android.graphics.drawable.Drawable
import android.widget.ImageView
import com.bumptech.glide.request.target.ImageViewTarget
import com.bumptech.glide.request.transition.Transition
import com.waz.utils.events.{NoAutowiring, Signal, SourceSignal}
import com.waz.zclient.glide.ObservableImageTarget._

class ObservableImageTarget(view: ImageView) extends ImageViewTarget[Drawable](view) {

  val resourceState: SourceSignal[ResourceState] with NoAutowiring = Signal(ResourceState.Cleared)

  override def setResource(resource: Drawable): Unit =
    view.setImageDrawable(resource)

  override def onLoadStarted(placeholder: Drawable): Unit = {
    super.onLoadStarted(placeholder)
    resourceState ! ResourceState.Started
  }

  override def onLoadFailed(errorDrawable: Drawable): Unit = {
    super.onLoadFailed(errorDrawable)
    resourceState ! ResourceState.Failed
  }

  override def onLoadCleared(placeholder: Drawable): Unit = {
    super.onLoadCleared(placeholder)
    resourceState ! ResourceState.Cleared
  }

  override def onResourceReady(resource: Drawable, transition: Transition[_ >: Drawable]): Unit = {
    super.onResourceReady(resource, transition)
    resourceState ! ResourceState.Ready
  }
}

object ObservableImageTarget {
  object ResourceState extends Enumeration {
    val Started, Failed, Cleared, Ready = Value
  }
  type ResourceState = ResourceState.Value

  def apply(view: ImageView): ObservableImageTarget = new ObservableImageTarget(view)
}
