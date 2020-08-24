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
    "40" -> "assets-transferred",
    "48" -> "payments-employee",
    "54" -> "vouchers-credit-cards",
    "38" -> "living-accommodation",
    "44" -> "mileage",
    "31" -> "car",
    "35" -> "vans",
    "36" -> "van-fuel",
    "37" -> "interest-free-loans",
    "30" -> "medical",
    "50" -> "qualifying-relocation",
    "8"  -> "services",
    "39" -> "assets-disposal",
    "47" -> "other",
    "52" -> "income-tax",
    "53" -> "travelling-subsistence",
    "42" -> "entertainment",
    "43" -> "business-travel",
    "32" -> "telephone",
    "45" -> "non-qualifying-relocation"
  )

  val urlMappedIABDValues2 = Map(
    "assets-transferred"        -> "40",
    "payments-employee"         -> "48",
    "vouchers-credit-cards"     -> "54",
    "living-accommodation"      -> "38",
    "mileage"                   -> "44",
    "car"                       -> "31",
    "vans"                      -> "35",
    "van-fuel"                  -> "36",
    "interest-free-loans"       -> "37",
    "medical"                   -> "30",
    "qualifying-relocation"     -> "50",
    "services"                  -> "8",
    "assets-disposal"           -> "39",
    "other"                     -> "47",
    "income-tax"                -> "52",
    "travelling-subsistence"    -> "53",
    "entertainment"             -> "42",
    "business-travel"           -> "43",
    "telephone"                 -> "32",
    "non-qualifying-relocation" -> "45"
  )

  def translate(number: String): String =
    urlMappedIABDValues.get(number).getOrElse(s"No translation for: $number ")

  def reverseTranslate(text: String): String =
    urlMappedIABDValues2.get(text).getOrElse(s"No translation for: $text")

}
