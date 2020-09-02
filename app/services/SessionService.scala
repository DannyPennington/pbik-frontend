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

package services

import config._
import javax.inject.Inject
import models._
import play.api.Logger
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.bootstrap.http.DefaultHttpClient

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SessionService @Inject()(
  val http: DefaultHttpClient,
  val sessionCache: PbikSessionCache,
  appConfig: PbikAppConfig) {

  private object CacheKeys extends Enumeration {
    val RegistrationList, BikRemoved, ListOfMatches, EiLPerson = Value
  }

  val PBIK_SESSION_KEY: String = "pbik_session"
  val cleanRegistrationList: Option[RegistrationList] = Some(RegistrationList(None, List.empty[RegistrationItem], None))
  val cleanBikRemoved: Option[RegistrationItem] = Some(RegistrationItem("", false, false))
  val cleanListOfMatches: Option[List[EiLPerson]] = Some(List.empty[EiLPerson])
  val cleanEiLPerson: Option[EiLPerson] = Some(EiLPerson("", "", None, "", None, None, None, None))
  val cleanSession: PbikSession =
    PbikSession(cleanRegistrationList, cleanBikRemoved, cleanListOfMatches, cleanEiLPerson)

  def fetchPbikSession()(implicit hc: HeaderCarrier): Future[Option[PbikSession]] =
    sessionCache.fetchAndGetEntry[PbikSession](PBIK_SESSION_KEY).recover {
      case ex: Exception =>
        Logger.error(s"[SessionService][fetchPbikSession] Fetch failed due to: $ex")
        None
    }

  def cacheRegistrationList(value: RegistrationList)(implicit hc: HeaderCarrier): Future[Option[PbikSession]] =
    cache(CacheKeys.RegistrationList, Some(value))

  def cacheBikRemoved(value: RegistrationItem)(implicit hc: HeaderCarrier): Future[Option[PbikSession]] =
    cache(CacheKeys.BikRemoved, Some(value))

  def cacheListOfMatches(value: List[EiLPerson])(implicit hc: HeaderCarrier): Future[Option[PbikSession]] =
    cache(CacheKeys.ListOfMatches, Some(value))

  def cacheEiLPerson(value: EiLPerson)(implicit headerCarrier: HeaderCarrier): Future[Option[PbikSession]] =
    cache(CacheKeys.EiLPerson, Some(value))

  def resetRegistrationList()(implicit hc: HeaderCarrier): Future[Option[PbikSession]] =
    cache(CacheKeys.RegistrationList)

  def resetBikRemoved()(implicit hc: HeaderCarrier): Future[Option[PbikSession]] =
    cache(CacheKeys.BikRemoved)

  def resetListOfMatches()(implicit hc: HeaderCarrier): Future[Option[PbikSession]] =
    cache(CacheKeys.ListOfMatches)

  def resetEiLPerson()(implicit headerCarrier: HeaderCarrier): Future[Option[PbikSession]] =
    cache(CacheKeys.EiLPerson)

  def resetAll()(implicit headerCarrier: HeaderCarrier): Future[Option[PbikSession]] = {
    resetBikRemoved()
    resetEiLPerson()
    resetRegistrationList()
    resetListOfMatches()
  }

  private def cache[T](key: CacheKeys.Value, value: Option[T] = None)(
    implicit hc: HeaderCarrier): Future[Option[PbikSession]] = {
    def selectKeysToCache(session: PbikSession): PbikSession =
      key match {
        case CacheKeys.RegistrationList => session.copy(registrations = Some(value.get.asInstanceOf[RegistrationList]))
        case CacheKeys.BikRemoved       => session.copy(bikRemoved = Some(value.get.asInstanceOf[RegistrationItem]))
        case CacheKeys.ListOfMatches    => session.copy(listOfMatches = Some(value.get.asInstanceOf[List[EiLPerson]]))
        case CacheKeys.EiLPerson        => session.copy(eiLPerson = Some(value.get.asInstanceOf[EiLPerson]))
        case _ =>
          Logger.warn(s"No matching keys")
          cleanSession
      }
    for {
      currentSession <- fetchPbikSession
      session = currentSession.getOrElse(cleanSession)
      cacheMap <- sessionCache.cache[PbikSession](PBIK_SESSION_KEY, selectKeysToCache(session))

    } yield {
      cacheMap.getEntry[PbikSession](PBIK_SESSION_KEY)
    }
  }

}
