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

package controllers.auth

import config.AppConfig
import controllers.FakePBIKApplication
import org.specs2.mock.Mockito
import support.AuthorityUtils._
import play.api.test.Helpers._
import play.api.mvc._
import play.api.test.{FakeApplication, FakeRequest}
import play.filters.csrf.CSRF
import play.filters.csrf.CSRF.UnsignedTokenProvider
import uk.gov.hmrc.domain.EmpRef
import uk.gov.hmrc.play.audit.http.HeaderCarrier
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import connectors.FrontendAuthConnector
import uk.gov.hmrc.play.frontend.auth.{Principal, LoggedInUser, AuthContext}
import uk.gov.hmrc.play.frontend.auth.connectors.domain.{LevelOfAssurance, Authority, Accounts, EpayeAccount}
import uk.gov.hmrc.play.frontend.auth.connectors.domain.LevelOfAssurance._
import uk.gov.hmrc.play.test.UnitSpec
import scala.concurrent.Future

object UserBuilder {

  val epayeAccount = Some(EpayeAccount(empRef = EmpRef(taxOfficeNumber = "taxOfficeNumber", taxOfficeReference ="taxOfficeReference" ), link =""))
  val accounts = Accounts(epaye = epayeAccount)
  val authority = new Authority("", accounts,None,None)
  val user = LoggedInUser(userId = "testUserId", None, None, None, LevelOfAssurance(2))
  val principal = Principal(name = Some("TEST_USER"), accounts)

  def apply() = {
    //User(userId = "testUserId", userAuthority = epayeAuthority("testUserId", "emp/ref"), nameFromGovernmentGateway = Some("TEST_USER"), decryptedToken = None)
    new AuthContext(user, principal, None)
  }

}

class AuthControllerSpec extends UnitSpec with Mockito with FakePBIKApplication {

  class SetUp {
    implicit val hc = HeaderCarrier()
    implicit def user = UserBuilder()

    def csrfToken = CSRF.TokenName -> UnsignedTokenProvider.generateToken
    def fakeRequest = FakeRequest().withSession(csrfToken)
    def fakeAuthenticatedRequest = FakeRequest().withSession(csrfToken).withHeaders()

  }

  class TestController extends  AuthController {
    override lazy val pbikAppConfig = mock[AppConfig]
    override protected implicit def authConnector = FrontendAuthConnector
  }


  "When an invalid user logs in, notAuthorised" should {
    "redirect to the authenticaiton page " in new SetUp {
      val controller = new TestController()
      val result: Future[Result] = await(controller.notAuthorised().apply(fakeRequest))
      status(result) shouldBe 303
      redirectLocation(result).get shouldBe
        "http://localhost:9025/payrollbik/sign-in?continue=http://localhost:9233/payrollbik/payrolled-benefits-expenses"
      val bodyText: String = contentAsString(result)
      //assert(bodyText.contains("Sign in with your Government Gateway account"))
    }
  }

  "When an valid user logs in, but their action is not Authorised" should {
    "redirect to the not authorised page " in new SetUp {
      val controller = new TestController()
      implicit val testac = user
      implicit val testRequest = fakeRequest
      val result: Future[Result] = await(controller.notAuthorisedResult)
      val bodyText: String = contentAsString(result)
      assert(bodyText.contains("Enrol to use this service"))
    }
  }

  "When an valid user logs in, and their action is  Authorised" should {
    "be status 200 " in new SetUp {
      val controller = new TestController()
      implicit val testac = user
      implicit val testRequest = fakeRequest
      val result: Future[Result] = await(controller.notAuthorisedResult)
      val bodyText: String = contentAsString(result)
      assert(bodyText.contains("Enrol to use this service"))
      assert(bodyText.contains("You are signed in to HMRC Online Services but your employer must enrol for employer Pay As You Earn before you can continue."))
      assert(bodyText.contains("To enrol you&#x27;ll need:"))
      assert(bodyText.contains("employer PAYE reference"))
      assert(bodyText.contains("Accounts office reference"))
      assert(bodyText.contains("You will then be sent an activation code in the post. When you receive it, log on again and use it to confirm your enrolment."))
      assert(bodyText.contains("You will then be able to use Payrolling Benefits in Kind."))
    }
  }

}