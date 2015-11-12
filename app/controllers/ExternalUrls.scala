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

package controllers

import play.api.Play
import uk.gov.hmrc.play.config.RunMode

object ExternalUrls extends RunMode {

  val loginCallback   = Play.current.configuration.getString("microservice.auth.login-callback.url").getOrElse(routes.HomePageController.onPageLoad().url)
  val companyAuthHost = Play.current.configuration.getString("microservice.auth.company-auth.host").getOrElse("")

  val signIn                      = s"$companyAuthHost/payrollbik/sign-in?continue=$loginCallback"
  val signInLocal                 = s"$companyAuthHost/payrollbik/sign-in-local?continue=$loginCallback"
  val signOut                     = s"$companyAuthHost/payrollbik/sign-out"

}