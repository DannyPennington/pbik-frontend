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

package controllers.registration

import config.PbikAppConfig
import connectors.HmrcTierConnector
import controllers.actions.{AuthAction, NoSessionCheckAction}
import services.{BikListService, RegistrationService, SessionService}
import javax.inject.Inject
import models._
import play.api.data.Form
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.Logger
import play.api.mvc._
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import utils.{ControllersReferenceData, URIInformation, _}
import views.html.registration.{ConfirmAddCurrentTaxYear, ConfirmUpdateNextTaxYear, CurrentTaxYear, NextTaxYear}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ManageRegistrationController @Inject()(
  bikListUtils: BikListUtils,
  registrationService: RegistrationService,
  formMappings: FormMappings,
  override val messagesApi: MessagesApi,
  cc: MessagesControllerComponents,
  val bikListService: BikListService,
  tierConnector: HmrcTierConnector,
  val authenticate: AuthAction,
  val noSessionCheck: NoSessionCheckAction,
  val cachingService: SessionService,
  controllersReferenceData: ControllersReferenceData,
  splunkLogger: SplunkLogger,
  pbikAppConfig: PbikAppConfig,
  uriInformation: URIInformation,
  nextTaxYearView: NextTaxYear,
  currentTaxYearView: CurrentTaxYear,
  confirmAddCurrentTaxYearView: ConfirmAddCurrentTaxYear,
  confirmUpdateNextTaxYearView: ConfirmUpdateNextTaxYear)
    extends FrontendController(cc) with I18nSupport {

  def nextTaxYearAddOnPageLoad: Action[AnyContent] =
    (authenticate andThen noSessionCheck).async { implicit request =>
      val staticDataRequest = registrationService.generateViewForBikRegistrationSelection(
        controllersReferenceData.YEAR_RANGE.cy,
        cachingSuffix = "add",
        generateViewBasedOnFormItems =
          nextTaxYearView(_, true, controllersReferenceData.YEAR_RANGE, _, _, _, _, _, empRef = request.empRef)
      )
      controllersReferenceData.responseErrorHandler(staticDataRequest)
    }

  /*
  These 2 are never actually called by anything apart from unit tests

  def nextTaxYearRemoveOnPageLoad: Action[AnyContent] = (authenticate andThen noSessionCheck).async {
    implicit request =>
      val staticDataRequest = loadNextTaxYearOnRemoveData
      controllersReferenceData.responseErrorHandler(staticDataRequest)
  }

  def loadNextTaxYearOnRemoveData(
    implicit request: AuthenticatedRequest[AnyContent],
    hc: HeaderCarrier): Future[Result] = {
    val taxYearRange = taxDateUtils.getTaxYearRange()
    val loadResultFuture = for {
      registeredListOption <- tierConnector.genericGetCall[List[Bik]](
                               uriInformation.baseUrl,
                               uriInformation.getRegisteredPath,
                               request.empRef,
                               controllersReferenceData.YEAR_RANGE.cy)
    } yield {
      val fetchFromCacheMapBiksValue = List.empty[RegistrationItem]
      val initialData = RegistrationList(None, registeredListOption.map { x =>
        RegistrationItem(x.iabdType, active = false, enabled = true)
      })
      val sortedData = bikListUtils.sortRegistrationsAlphabeticallyByLabels(initialData)
      if (sortedData.active.isEmpty) {
        Ok(
          errorPageView(
            ControllersReferenceDataCodes.NO_MORE_BENEFITS_TO_REMOVE_CY1,
            taxYearRange,
            FormMappingsConstants.CYP1,
            -1,
            "Registered benefits for tax year starting 6 April " + controllersReferenceData.YEAR_RANGE.cy,
            "manage-registrations",
            empRef = Some(request.empRef)
          ))
      } else {
        Ok(
          nextTaxYearView(
            bikForm = formMappings.objSelectedForm.fill(sortedData),
            additive = false,
            taxYearRange = taxYearRange,
            previouslySelectedBenefits = fetchFromCacheMapBiksValue,
            registeredBiks = List.empty[Bik],
            nonLegislationBiks = List.empty[Int],
            decommissionedBiks = List.empty[Int],
            biksAvailableCount = None,
            empRef = request.empRef
          ))
      }
    }
    controllersReferenceData.responseErrorHandler(loadResultFuture)
  }

   */

  def currentTaxYearOnPageLoad: Action[AnyContent] =
    (authenticate andThen noSessionCheck).async { implicit request =>
      val staticDataRequest = registrationService.generateViewForBikRegistrationSelection(
        controllersReferenceData.YEAR_RANGE.cyminus1,
        cachingSuffix = "add",
        generateViewBasedOnFormItems =
          currentTaxYearView(_, controllersReferenceData.YEAR_RANGE, _, _, _, _, _, empRef = request.empRef)
      )
      controllersReferenceData.responseCheckCYEnabled(staticDataRequest)
    }

  def checkYourAnswersAddCurrentTaxYear: Action[AnyContent] = (authenticate andThen noSessionCheck).async {
    implicit request =>
      val resultFuture = for {
        biksListOption: List[Bik] <- bikListService.registeredBenefitsList(
                                      controllersReferenceData.YEAR_RANGE.cyminus1,
                                      EmpRef.empty)(uriInformation.getBenefitTypesPath)
        result <- formMappings.objSelectedForm.bindFromRequest.fold(
                   formWithErrors =>
                     Future.successful(
                       Ok(currentTaxYearView(
                         formWithErrors,
                         controllersReferenceData.YEAR_RANGE,
                         registeredBiks = List.empty[Bik],
                         nonLegislationBiks = pbikAppConfig.biksNotSupportedCY,
                         decommissionedBiks = pbikAppConfig.biksDecommissioned,
                         biksAvailableCount = Some(biksListOption.size),
                         empRef = request.empRef
                       ))),
                   values => {
                     cachingService.cacheRegistrationList(values).flatMap { _ =>
                       Future.successful(
                         Redirect(routes.ManageRegistrationController.showCheckYourAnswersAddCurrentTaxYear()))
                     }
                   }
                 )
      } yield result
      controllersReferenceData.responseCheckCYEnabled(resultFuture)
  }

  def showCheckYourAnswersAddCurrentTaxYear: Action[AnyContent] = (authenticate andThen noSessionCheck).async {
    implicit request =>
      val resultFuture = cachingService.fetchPbikSession().flatMap { session =>
        val registrationList = RegistrationList(None, session.get.registrations.get.active.filter(_.active), None)
        val form: Form[RegistrationList] = formMappings.objSelectedForm.fill(registrationList)
        Future.successful(
          Ok(confirmAddCurrentTaxYearView(form, controllersReferenceData.YEAR_RANGE, empRef = request.empRef)))
      }
      controllersReferenceData.responseErrorHandler(resultFuture)
  }

  def checkYourAnswersAddNextTaxYear: Action[AnyContent] = (authenticate andThen noSessionCheck).async {
    implicit request =>
      val resultFuture = for {
        biksListOption: List[Bik] <- bikListService.registeredBenefitsList(
                                      controllersReferenceData.YEAR_RANGE.cy,
                                      EmpRef.empty)(uriInformation.getBenefitTypesPath)
        result <- formMappings.objSelectedForm.bindFromRequest.fold(
                   formWithErrors =>
                     Future.successful(
                       Ok(nextTaxYearView(
                         bikForm = formWithErrors,
                         additive = true,
                         taxYearRange = controllersReferenceData.YEAR_RANGE,
                         nonLegislationBiks = pbikAppConfig.biksNotSupported,
                         decommissionedBiks = pbikAppConfig.biksDecommissioned,
                         biksAvailableCount = Some(biksListOption.size),
                         empRef = request.empRef
                       ))),
                   values => {
                     cachingService.cacheRegistrationList(values).flatMap { _ =>
                       Future.successful(
                         Redirect(routes.ManageRegistrationController.showCheckYourAnswersAddNextTaxYear()))
                     }
                   }
                 )
      } yield result
      controllersReferenceData.responseErrorHandler(resultFuture)
  }

  def showCheckYourAnswersAddNextTaxYear: Action[AnyContent] = (authenticate andThen noSessionCheck).async {
    implicit request =>
      val resultFuture = cachingService.fetchPbikSession().flatMap { session =>
        val registrationList = RegistrationList(None, session.get.registrations.get.active.filter(_.active), None)
        val form: Form[RegistrationList] = formMappings.objSelectedForm.fill(registrationList)
        Future.successful(
          Ok(
            confirmUpdateNextTaxYearView(
              registrationList,
              None,
              additive = true,
              controllersReferenceData.YEAR_RANGE,
              empRef = request.empRef)))
      }
      controllersReferenceData.responseErrorHandler(resultFuture)
  }

  /*
  This is never actually used - it seems there is code in the NextTaxYear view alongside this that would support multiple removals at the same time,
  the same way it does with adding multiple benefits. However this has never been enabled and is impossible to reach in the current service.
  Enabling it would require a bit of a re-write anyway so my vote would be to remove this entirely as it makes the service more confusing to work with

  def confirmRemoveNextTaxYear: Action[AnyContent] = (authenticate andThen noSessionCheck).async { implicit request =>
    val resultFuture = for {
      session <- cachingService.fetchPbikSession()
      result <- generateConfirmationScreenView(
                 controllersReferenceData.YEAR_RANGE.cy,
                 cachingSuffix = "remove",
                 generateViewBasedOnFormItems = confirmUpdateNextTaxYearView(
                   session.get.registrations.get,
                   session.get.bikRemoved,
                   _,
                   additive = false,
                   controllersReferenceData.YEAR_RANGE,
                   empRef = request.empRef),
                 viewToRedirect = formWithErrors =>
                   Ok(
                     nextTaxYearView(
                       bikForm = formWithErrors,
                       additive = false,
                       taxYearRange = controllersReferenceData.YEAR_RANGE,
                       nonLegislationBiks = pbikAppConfig.biksNotSupported,
                       decommissionedBiks = pbikAppConfig.biksDecommissioned,
                       biksAvailableCount = None,
                       empRef = request.empRef
                     ))
               )
    } yield {
      result
    }
    controllersReferenceData.responseErrorHandler(resultFuture)
  }

    def generateConfirmationScreenView(
    year: Int,
    cachingSuffix: String,
    generateViewBasedOnFormItems: Form[RegistrationList] => HtmlFormat.Appendable,
    viewToRedirect: Form[RegistrationList] => Result)(
    implicit hc: HeaderCarrier,
    request: Request[AnyContent]): Future[Result] =
    formMappings.objSelectedForm.bindFromRequest.fold(
      formWithErrors => Future.successful(viewToRedirect(formWithErrors)),
      values => {
        val items: List[RegistrationItem] = values.active.filter(x => x.active)
        Future.successful(
          Ok(generateViewBasedOnFormItems(formMappings.objSelectedForm.fill(RegistrationList(None, items, None)))))
      }
    )
   */

  def confirmRemoveNextTaxYearNoForm(iabdType: String): Action[AnyContent] =
    (authenticate andThen noSessionCheck).async { implicit request =>
      val bikToRemove = RegistrationItem(iabdType, active = true, enabled = true)
      val registrationList =
        RegistrationList(None, List(bikToRemove), reason = None)
      cachingService.cacheBikRemoved(RegistrationItem(uriInformation.iabdValueURLDeMapper(iabdType), false, true))
      val resultFuture = Future.successful(
        Ok(
          confirmUpdateNextTaxYearView(
            registrationList,
            Some(bikToRemove),
            additive = false,
            controllersReferenceData.YEAR_RANGE,
            empRef = request.empRef
          )))
      controllersReferenceData.responseErrorHandler(resultFuture)
    }

  def removeNextYearRegisteredBenefitTypes: Action[AnyContent] = (authenticate andThen noSessionCheck).async {
    implicit request =>
      val registeredFuture = cachingService.fetchPbikSession().flatMap { session =>
        val bikToRemove = Bik(session.get.bikRemoved.get.id, ControllersReferenceDataCodes.BIK_REMOVE_STATUS)
        updateBiksFutureAction(controllersReferenceData.YEAR_RANGE.cy, List(bikToRemove), additive = false)
      }
      controllersReferenceData.responseErrorHandler(registeredFuture)
  }

  def updateCurrentYearRegisteredBenefitTypes: Action[AnyContent] = (authenticate andThen noSessionCheck).async {
    implicit request =>
      val actionFuture = cachingService.fetchPbikSession().flatMap { session =>
        val persistentBiks = session.get.registrations.get.active
          .filter(x => x.active)
          .map(x => Bik(x.id, ControllersReferenceDataCodes.BIK_ADD_STATUS))
        updateBiksFutureAction(controllersReferenceData.YEAR_RANGE.cyminus1, persistentBiks, additive = true)
      }
      controllersReferenceData.responseCheckCYEnabled(actionFuture)
  }

  def addNextYearRegisteredBenefitTypes: Action[AnyContent] = (authenticate andThen noSessionCheck).async {
    implicit request =>
      val actionFuture = cachingService.fetchPbikSession().flatMap { session =>
        val persistentBiks = session.get.registrations.get.active
          .filter(x => x.active)
          .map(x => Bik(x.id, ControllersReferenceDataCodes.BIK_ADD_STATUS))
        updateBiksFutureAction(controllersReferenceData.YEAR_RANGE.cy, persistentBiks, additive = true)
      }
      controllersReferenceData.responseErrorHandler(actionFuture)
  }

  def updateBiksFutureAction(year: Int, persistentBiks: List[Bik], additive: Boolean)(
    implicit request: AuthenticatedRequest[AnyContent]): Future[Result] =
    tierConnector
      .genericGetCall[List[Bik]](uriInformation.baseUrl, uriInformation.getRegisteredPath, request.empRef, year)
      .flatMap { registeredResponse =>
        cachingService.fetchPbikSession().flatMap { session =>
          val changes = bikListUtils.normaliseSelectedBenefits(registeredResponse, persistentBiks)
          if (additive) {
            // Process registration
            val saveFuture = tierConnector.genericPostCall(
              uriInformation.baseUrl,
              uriInformation.updateBenefitTypesPath,
              request.empRef,
              year,
              changes)
            saveFuture.map { saveResponse: HttpResponse =>
              auditBikUpdate(additive = true, year, persistentBiks)
              lazy val yearRange = controllersReferenceData.YEAR_RANGE
              lazy val yearString = year match {
                case yearRange.cy       => "cy1"
                case yearRange.cyminus1 => "cy"
              }
              Redirect(controllers.routes.WhatNextPageController.showWhatNextRegisteredBik(yearString))
            }
          } else {
            // Remove benefit - if there are no errors proceed
            formMappings.removalReasonForm
              .bindFromRequest()
              .fold(
                _ => {
                  Logger.warn("[ManageRegistrationController][updateBiksFutureAction] No removal reason selected")
                  Future.successful(
                    Redirect(routes.ManageRegistrationController.confirmRemoveNextTaxYearNoForm(
                      uriInformation.iabdValueURLMapper(persistentBiks.head.iabdType)))
                      .flashing("error" -> Messages("RemoveBenefits.reason.no.selection")))
                },
                values => {
                  val listWithReason =
                    RegistrationList(None, session.get.registrations.get.active, reason = Some(values))
                  cachingService.cacheRegistrationList(listWithReason).flatMap { _ =>
                    Future.successful(removeBenefitReasonValidation(listWithReason, year, persistentBiks, changes))
                  }
                }
              )
          }
        }
      }

  def removeBenefitReasonValidation(
    registrationList: RegistrationList,
    year: Int,
    persistentBiks: List[Bik],
    changes: List[Bik])(implicit request: AuthenticatedRequest[AnyContent]): Result =
    registrationList.reason match {
      case Some(reasonValue)
          if ControllersReferenceDataCodes.BIK_REMOVE_REASON_LIST.contains(reasonValue.selectionValue) => {
        reasonValue.info match {
          case _ if reasonValue.selectionValue.equals("other") && reasonValue.info.getOrElse("").trim.isEmpty => {
            val message: Flash = Flash(
              Map(("error", Messages("RemoveBenefits.reason.other.required")), ("test", "test")))
            Redirect(
              routes.ManageRegistrationController.confirmRemoveNextTaxYearNoForm(
                uriInformation.iabdValueURLMapper(persistentBiks.head.iabdType))).flashing(message)
          }
          case Some(info) => {
            tierConnector.genericPostCall(
              uriInformation.baseUrl,
              uriInformation.updateBenefitTypesPath,
              request.empRef,
              year,
              changes)
            auditBikUpdate(
              additive = false,
              year,
              persistentBiks,
              Some((reasonValue.selectionValue.toUpperCase, Some(info))))
            Redirect(controllers.routes.WhatNextPageController.showWhatNextRemovedBik())
          }
          case _ => {
            tierConnector.genericPostCall(
              uriInformation.baseUrl,
              uriInformation.updateBenefitTypesPath,
              request.empRef,
              year,
              changes)
            auditBikUpdate(additive = false, year, persistentBiks, Some((reasonValue.selectionValue.toUpperCase, None)))
            Redirect(controllers.routes.WhatNextPageController.showWhatNextRemovedBik())
          }
        }
      }
      case _ => {
        Logger.warn(
          s"[ManageRegistrationController][removeBenefitReasonValidation] Couldn't find reason from request form")
        Redirect(
          routes.ManageRegistrationController.confirmRemoveNextTaxYearNoForm(
            uriInformation.iabdValueURLMapper(persistentBiks.head.iabdType)))
          .flashing("error" -> Messages("RemoveBenefits.reason.no.selection"))
      }
    }

  private def auditBikUpdate(
    additive: Boolean,
    year: Int,
    persistentBiks: List[Bik],
    removeReason: Option[(String, Option[String])] = None)(
    implicit hc: HeaderCarrier,
    request: AuthenticatedRequest[_]): Unit = {
    val derivedMsg =
      if (additive) "Benefit added to " + splunkLogger.taxYearToSpPeriod(year)
      else "Benefit removed from " + splunkLogger.taxYearToSpPeriod(year)
    for (bik <- persistentBiks) {
      splunkLogger.logSplunkEvent(
        splunkLogger.createDataEvent(
          tier = splunkLogger.FRONTEND,
          action = if (additive) splunkLogger.ADD else splunkLogger.REMOVE,
          target = splunkLogger.BIK,
          period = splunkLogger.taxYearToSpPeriod(year),
          msg = derivedMsg + " : " + bik.iabdType,
          nino = None,
          iabd = Some(bik.iabdType),
          removeReason = if (additive) None else Some(removeReason.get._1),
          removeReasonDesc = if (additive) None else Some(removeReason.get._2.getOrElse("")),
          name = Some(request.name),
          empRef = Some(request.empRef)
        ))
    }
  }

}
