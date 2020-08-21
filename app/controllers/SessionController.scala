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

import config.PbikAppConfig
import javax.inject.Inject
import play.api.Logger
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{SessionService}
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}

class SessionController @Inject()(
  val authConnector: DefaultAuthConnector,
  val sessionService: SessionService,
  val mcc: MessagesControllerComponents,
  val appConfig: PbikAppConfig)
    extends FrontendController(mcc) {

  implicit val ec: ExecutionContext = mcc.executionContext

  val CAR_AND_FUEL = "car-and-fuel"
  val PRIVATE_INSURANCE = "private-insurance"
  val EMPLOYEE_ASSETS = "employee-assets"
  val ASSETS_TRANSFERRED = "assets-transferred"
  val ENTERTAINMENT = "entertainment"
  val INCOME_TAX = "income-tax"
  val MILEAGE_ALLOWANCE = "mileage-allowance"
  val NON_QUALIFYING_RELOCATION_EXPENSES = "non-qualifying-relocation-expenses"
  val OTHER_ITEMS = "other-items"
  val HOME_TELEPHONE = "home-telephone"
  val EMPLOYEE_PAYMENTS = "employee-payments"
  val QUALIFYING_RELOCATION_EXPENSES = "qualifying-relocation-expenses"
  val SERVICES_SUPPLIED = "services-supplied"
  val TRAVELLING_PAYMENTS = "travelling-payments"
  val VAN_FUEL = "van-fuel"
  val VANS = "vans"
  val VOUCHERS = "vouchers"
}
