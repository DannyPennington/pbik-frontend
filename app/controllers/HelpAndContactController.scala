/*
 * Copyright 2017 HM Revenue & Customs
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

import java.net.URLEncoder

import config.PbikAppConfig
import connectors.{WSHttp, PBIKHeaderCarrierForPartialsConverter, HmrcTierConnector, TierConnector}
import controllers.auth._
import play.api.Logger
import play.api.mvc._
import play.twirl.api.{HtmlFormat, Html}
import services.BikListService
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.controller.FrontendController
import utils._
import uk.gov.hmrc.play.http.{HeaderCarrier, _}
import play.api.i18n.Messages.Implicits._
import play.api.Play.current
import scala.concurrent.Future

object HelpAndContactController extends HelpAndContactController with TierConnector
with AuthenticationConnector with Actions {
  override val httpPost = WSHttp

  def pbikAppConfig = PbikAppConfig
  def bikListService = BikListService
  val tierConnector = new HmrcTierConnector

  override val contactFrontendPartialBaseUrl = pbikAppConfig.contactFrontendService
  override val contactFormServiceIdentifier = pbikAppConfig.contactFormServiceIdentifier
}

trait HelpAndContactController extends FrontendController with URIInformation
with ControllersReferenceData with PbikActions with EpayeUser with SplunkLogger  {

  this: TierConnector =>
  def bikListService: BikListService

  def httpPost: HttpPost
  private val TICKET_ID = "ticketId"

  def contactFrontendPartialBaseUrl: String
  def contactFormServiceIdentifier: String

  private lazy val submitUrl = routes.HelpAndContactController.submitContactHmrcForm().url
  private lazy val contactHmrcFormPartialUrl = s"$contactFrontendPartialBaseUrl/contact/contact-hmrc/form?service=${contactFormServiceIdentifier}&submitUrl=${urlEncode(submitUrl)}&renderFormOnly=true"
  private lazy val contactHmrcSubmitPartialUrl = s"$contactFrontendPartialBaseUrl/contact/contact-hmrc/form?resubmitUrl=${urlEncode(submitUrl)}&renderFormOnly=true"


  private def urlEncode(value: String) = URLEncoder.encode(value, "UTF-8")

  private def partialsReadyHeaderCarrier(implicit request: Request[_]): HeaderCarrier = {
    val hc1 = PBIKHeaderCarrierForPartialsConverter.headerCarrierEncryptingSessionCookieFromRequest(request)
    PBIKHeaderCarrierForPartialsConverter.headerCarrierForPartialsToHeaderCarrier(hc1)
  }

  def onPageLoad:Action[AnyContent] = AuthorisedForPbik {
    implicit ac =>
      implicit request =>
        Future.successful(Ok(views.html.helpcontact.helpContact(contactHmrcFormPartialUrl, None)))
  }

  def submitContactHmrcForm = AuthorisedForPbik {
    implicit ac =>
      implicit request =>

        submitContactHmrc(contactHmrcSubmitPartialUrl,
          routes.HelpAndContactController.confirmationContactHmrc(),
          (body: Html) => views.html.helpcontact.helpContact(contactHmrcFormPartialUrl, Some(body)))
  }

  def confirmationContactHmrc:Action[AnyContent] = AuthorisedForPbik {
    implicit ac =>
      implicit request =>
        Future.successful(Ok(views.html.helpcontact.confirmHelpContact()))
  }

  private def submitContactHmrc(formUrl: String, successRedirect: Call, failedValidationResponseContent: (Html) => HtmlFormat.Appendable)
                               (implicit request: Request[AnyContent]) : Future[Result] = {
    request.body.asFormUrlEncoded.map { formData =>
      httpPost.POSTForm[HttpResponse](formUrl, formData)(rds = PartialsFormReads.readPartialsForm, hc = partialsReadyHeaderCarrier).map {
        resp => resp.status match {
          case 200 => Redirect(successRedirect).withSession(request.session + (TICKET_ID -> resp.body))
          case 400 => BadRequest(failedValidationResponseContent(Html(resp.body)))
          case 500 => {
            Logger.warn("submit contact form internal error 500, " + resp.body)
            InternalServerError(Html(resp.body))
          }
          case status => throw new Exception(s"Unexpected status code from contact HMRC form: $status")
        }
      }
    }.getOrElse {
      Logger.warn("Trying to submit an empty contact form")
      Future.successful(InternalServerError)
    }
  }
}

object PartialsFormReads {
  implicit val readPartialsForm: HttpReads[HttpResponse] = new HttpReads[HttpResponse] {
    def read(method: String, url: String, response: HttpResponse) = response
  }
}
