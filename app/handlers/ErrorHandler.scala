/*
 * Copyright 2019 HM Revenue & Customs
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

package handlers

import config.PbikAppConfig
import javax.inject.Inject
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.Request
import play.twirl.api.Html
import uk.gov.hmrc.play.bootstrap.http.FrontendErrorHandler
import views.html.ErrorTemplate
import views.html.page_not_found_template

class ErrorHandler @Inject()(
  val messagesApi: MessagesApi,
  errorTemplateView: ErrorTemplate,
  notFoundTemplate: page_not_found_template)(implicit val config: PbikAppConfig)
    extends FrontendErrorHandler with I18nSupport {

  override def standardErrorTemplate(pageTitle: String, heading: String, message: String)(
    implicit request: Request[_]): Html =
    errorTemplateView(pageTitle, heading, message)

  override def notFoundTemplate(implicit request: Request[_]): Html =
    notFoundTemplate()

}
