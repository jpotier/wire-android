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

import android.content.Context
import android.graphics.drawable.{ColorDrawable, Drawable}
import android.graphics.{Canvas, Color, Paint}
import android.util.AttributeSet
import android.widget.ImageView
import com.waz.model.{AccentColor, AssetId, UserData, UserId}
import com.waz.service.ZMessaging
import com.waz.utils.events.Signal
import com.waz.utils.{NameParts, returning}
import com.waz.zclient.common.views.NewChatHead._
import com.waz.zclient.glide.GlideDrawable
import com.waz.zclient.glide.transformations.GreyScaleTransformation
import com.waz.zclient.ui.utils.TypefaceUtils
import com.waz.zclient.utils.ContextUtils.getString
import com.waz.zclient.{GlideRequest, R, ViewHelper}

class NewChatHead(val context: Context, val attrs: AttributeSet, val defStyleAttr: Int) extends ImageView(context, attrs, defStyleAttr) with ViewHelper {
  def this(context: Context, attrs: AttributeSet) = this(context, attrs, 0)
  def this(context: Context) = this(context, null)

  private val zms = inject[Signal[ZMessaging]]
  private val userId = Signal[UserId]()
  val attributes: Attributes = parseAttributes(attrs)

  private val options = for {
    z <- zms
    uId <- userId
    user <- z.usersStorage.signal(uId)
  } yield optionsForUser(user, z.teamId.exists(user.teamId.contains(_)), attributes)

  options.orElse(Signal.const(defaultOptions(attributes))).onUi(setInfo)

  def parseAttributes(attributeSet: AttributeSet): Attributes = {
    val a = context.getTheme.obtainStyledAttributes(attributeSet, R.styleable.ChatheadView, 0, 0)

    val selectable = a.getBoolean(R.styleable.NewChatHead_isSelectable2, false)
    val isRound = a.getBoolean(R.styleable.NewChatHead_is_round2, true)
    val showWaiting = a.getBoolean(R.styleable.NewChatHead_show_waiting2, true)
    val grayScaleOnConnected = a.getBoolean(R.styleable.NewChatHead_gray_on_unconnected2, true)
    val defaultBackground = ColorVal(a.getColor(R.styleable.NewChatHead_default_background2, Color.GRAY)).value

    Attributes(selectable, isRound, showWaiting, grayScaleOnConnected, defaultBackground)
  }

  def setUserId(userId: UserId): Unit = this.userId ! userId

  def clearUser(): Unit = {} //TODO

  def setInfo(options: ChatHeadViewOptions): Unit = options.glideRequest.into(this)

  private def optionsForUser(user: UserData, teamMember: Boolean, attributes: Attributes): ChatHeadViewOptions = {
    val assetId = user.picture
    val backgroundColor = AccentColor.apply(user.accent).color
    val greyScale = !(user.isConnected || user.isSelf || user.isWireBot || teamMember) && attributes.greyScaleOnConnected
    val initials = NameParts.parseFrom(user.name).initials

    ChatHeadViewOptions(assetId, backgroundColor, greyScale, initials, attributes.isRound, attributes.selectable, attributes.showWaiting)
  }

  private def defaultOptions(attributes: Attributes): ChatHeadViewOptions =
    ChatHeadViewOptions(
      None,
      attributes.defaultBackground,
      grayScale = false,
      "",
      isRound = attributes.isRound,
      selectable = attributes.selectable,
      showWaiting = attributes.showWaiting)
}

object NewChatHead {
  case class Attributes(selectable: Boolean,
                        isRound: Boolean,
                        showWaiting: Boolean,
                        greyScaleOnConnected: Boolean,
                        defaultBackground: Int)

  case class ChatHeadViewOptions(assetId: Option[AssetId],
                                 backgroundColor: Int,
                                 grayScale: Boolean,
                                 initials: String,
                                 isRound: Boolean,
                                 selectable: Boolean,
                                 showWaiting: Boolean) {

    def glideRequest(implicit context: Context): GlideRequest[Drawable] = {
      val request = assetId match {
        case Some(id) => GlideDrawable(id)
        case _ => GlideDrawable(new ColorDrawable(Color.TRANSPARENT))
      }
      request.placeholder(new ChatHeadViewPlaceholder(backgroundColor, initials))
      if (isRound) request.circleCrop()
      if (grayScale) request.transform(new GreyScaleTransformation())
      request
    }
  }

  class ChatHeadViewPlaceholder(color: Int, text: String)(implicit context: Context) extends ColorDrawable(color) {

    private val textPaint = returning(new Paint(Paint.ANTI_ALIAS_FLAG)) { p =>
      p.setTextAlign(Paint.Align.CENTER)
      p.setTypeface(TypefaceUtils.getTypeface(getString(R.string.chathead__user_initials__font)))
      p.setColor(Color.WHITE)
    }

    override def draw(canvas: Canvas): Unit = {
      super.draw(canvas)

      val y = canvas.getHeight / 2 - ((textPaint.descent + textPaint.ascent) / 2f)
      val x = canvas.getWidth / 2
      canvas.drawText(text, x, y, textPaint)
    }
  }

}
