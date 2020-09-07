/*
 * Copyright 2020 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package controllers

import java.util.UUID

import connectors.HmrcTierConnector
import javax.inject.Inject
import models._
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.{BikListService, SessionService}
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import utils.{ControllersReferenceData, _}
import views.html.registration.WhatNextAddRemove
import controllers.actions.{AuthAction, NoSessionCheckAction}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class WhatNextPageController @Inject()(
  override val messagesApi: MessagesApi,
  val cachingService: SessionService,
  bikListService: BikListService,
  formMappings: FormMappings,
  authenticate: AuthAction,
  noSessionCheck: NoSessionCheckAction,
  val tierConnector: HmrcTierConnector,
  taxDateUtils: TaxDateUtils,
  controllersReferenceData: ControllersReferenceData,
  cc: MessagesControllerComponents,
  whatNextAddRemoveView: WhatNextAddRemove)
    extends FrontendController(cc) with I18nSupport {

  def calculateTaxYear(isCurrentTaxYear: Boolean): (Int, Int) = {
    val isCurrentYear = if (isCurrentTaxYear) FormMappingsConstants.CY else FormMappingsConstants.CYP1
    isCurrentYear match {
      case FormMappingsConstants.CY =>
        (controllersReferenceData.YEAR_RANGE.cyminus1, controllersReferenceData.YEAR_RANGE.cy)
      case FormMappingsConstants.CYP1 =>
        (controllersReferenceData.YEAR_RANGE.cy, controllersReferenceData.YEAR_RANGE.cyplus1)
    }
  }

  def showWhatNextRegisteredBik: Action[AnyContent] =
    (authenticate).async { implicit request =>
      val resultFuture = cachingService.fetchPbikSession().map { session =>
        Ok(
          whatNextAddRemoveView(
            taxDateUtils.isCurrentTaxYear(controllersReferenceData.YEAR_RANGE.cy),
            controllersReferenceData.YEAR_RANGE,
            additive = true,
            RegistrationList(active = session.get.registrations.get.active.filter(item => item.active)),
            empRef = request.empRef
          ))
      }
      controllersReferenceData.responseErrorHandler(resultFuture)
    }

  def showWhatNextRemovedBik: Action[AnyContent] =
    (authenticate).async { implicit request =>
      val resultFuture = cachingService.fetchPbikSession().map { session =>
        val removedBikAsList: RegistrationList = RegistrationList(active = List(session.get.bikRemoved.get))
        Ok(
          whatNextAddRemoveView(
            taxDateUtils.isCurrentTaxYear(controllersReferenceData.YEAR_RANGE.cyplus1),
            controllersReferenceData.YEAR_RANGE,
            additive = false,
            removedBikAsList,
            empRef = request.empRef
          ))
      }
      controllersReferenceData.responseErrorHandler(resultFuture)
    }
}
