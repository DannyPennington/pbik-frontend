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

class TranslatorService {

  val urlMappedIABDValues = Map(
    "40" -> "assets_transferred",
    "48" -> "employee_payments",
    "54" -> "vouchers",
    "38" -> "living-accommodation",
    "44" -> "mileage_allowance",
    "31" -> "car_and_fuel",
    "35" -> "vans",
    "36" -> "van_fuel",
    "37" -> "interest-free-loans",
    "30" -> "private_insurance",
    "50" -> "qualifying_relocation_expenses",
    "8"  -> "services_supplied",
    "39" -> "employee_assets",
    "47" -> "other_items",
    "52" -> "income_tax",
    "53" -> "travelling-payments",
    "42" -> "entertainment",
    "43" -> "business-travel",
    "32" -> "home_telephone",
    "45" -> "non_qualifying_relocation_expenses"
  )

  val urlMappedIABDValues2 = Map(
    "assets_transferred"                 -> "40",
    "employee_payments"                  -> "48",
    "vouchers"                           -> "54",
    "living-accommodation"               -> "38",
    "mileage_allowance"                  -> "44",
    "car_and_fuel"                       -> "31",
    "vans"                               -> "35",
    "van_fuel"                           -> "36",
    "interest-free-loans"                -> "37",
    "private_insurance"                  -> "30",
    "qualifying_relocation_expenses"     -> "50",
    "services_supplied"                  -> "8",
    "employee_assets"                    -> "39",
    "other_items"                        -> "47",
    "income_tax"                         -> "52",
    "travelling-payments"                -> "53",
    "entertainment"                      -> "42",
    "business-travel"                    -> "43",
    "home_telephone"                     -> "32",
    "non_qualifying_relocation_expenses" -> "45"
  )

  def translate(number: String): String =
    urlMappedIABDValues.get(number).getOrElse("No translation")

  def reverseTranslate(text: String): String =
    urlMappedIABDValues2.get(text).getOrElse("No translation")

}
