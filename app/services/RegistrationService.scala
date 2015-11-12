/*
 * Copyright 2015 HM Revenue & Customs
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

package services

import config.PbikAppConfig
import connectors.{HmrcTierConnector, TierConnector}
import controllers.WhatNextPageController
import controllers.registration.{ManageRegistrationController}
import controllers.auth.{AuthenticationConnector, EpayeUser, PbikActions}
import models.{Bik, RegistrationItem, RegistrationList}
import play.api.data.Form
import play.api.mvc.{Result, AnyContent, Request}
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.play.audit.http.HeaderCarrier
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.controller.FrontendController
import utils._

import scala.concurrent.Future

object RegistrationService extends RegistrationService with TierConnector with AuthenticationConnector {
  def pbikAppConfig = PbikAppConfig
  def bikListService = BikListService
  val tierConnector = new HmrcTierConnector
}

trait RegistrationService extends FrontendController with URIInformation
                  with ControllersReferenceData with WhatNextPageController with PbikActions
                  with EpayeUser {

  this: TierConnector =>

  def generateViewForBikRegistrationSelection(year: Int, cachingSuffix: String,
                                              generateViewBasedOnFormItems: (Form[RegistrationList],
                                                List[RegistrationItem], List[Bik], List[Int]) => HtmlFormat.Appendable)
                                             (implicit hc:HeaderCarrier, request: Request[AnyContent], ac: AuthContext):

  Future[Result] = {

    val nonLegislationBiks:List[Int] = PbikAppConfig.biksNotSupported

    val isCurrentYear:String = TaxDateUtils.isCurrentTaxYear(year) match {
      case true => FormMappingsConstants.CY
      case false => FormMappingsConstants.CYP1
    }

    for {
      biksListOption:List[Bik] <- bikListService.registeredBenefitsList(year, "")(getBenefitTypesPath)
      registeredListOption <- tierConnector.genericGetCall[List[Bik]](baseUrl, getRegisteredPath,
        ac.principal.accounts.epaye.get.empRef.toString, year)
      val nonLegislationList = nonLegislationBiks.map { x =>
        Bik(""+x, 30, 0)}
      val hybridList = biksListOption ::: (nonLegislationList)

    } yield {
      val pbikHeaders = bikListService.pbikHeaders
      val fetchFromCacheMapBiksValue = List.empty[RegistrationItem]

      val mergedData: RegistrationList = utils.BikListUtils.removeMatches(hybridList, registeredListOption)
      val sortedMegedData: RegistrationList =  utils.BikListUtils.sortRegistrationsAlphabeticallyByLabels(mergedData)
      if (sortedMegedData.active.size == 0) {
        Ok(views.html.errorPage(NO_MORE_BENEFITS_TO_ADD, YEAR_RANGE,
          isCurrentYear, -1, NO_MORE_BENEFITS_TO_ADD_HEADING))
      }
      else {
        Ok(generateViewBasedOnFormItems(objSelectedForm.fill(sortedMegedData),
          fetchFromCacheMapBiksValue, registeredListOption, PbikAppConfig.biksNotSupported))
      }

    }
  }
}