package uk.ac.wellcome.models.parse

import fastparse._, NoWhitespace._

import uk.ac.wellcome.models.work.internal.{IdState, Place}

/**
  *  Parses Marc country information
  *
  *  https://www.loc.gov/marc/countries/countries_code.html
  */
object MarcPlaceParser extends Parser[Place[IdState.Unminted]] {

  def parser[_: P] =
    AnyChar
      .rep(exactly = 3)
      .!
      .map(placeMap.get)
      .filter(_.nonEmpty)
      .map(_.get)
      .map(Place(_))

  val placeMap = Map(
    "aa " -> "Albania",
    "abc" -> "Alberta",
    "ac " -> "Ashmore and Cartier Islands",
    "aca" -> "Australian Capital Territory",
    "ae " -> "Algeria",
    "af " -> "Afghanistan",
    "ag " -> "Argentina",
    "ai " -> "Anguilla",
    "ai " -> "Armenia (Republic)",
    "air" -> "Armenian S.S.R.",
    "aj " -> "Azerbaijan",
    "ajr" -> "Azerbaijan S.S.R.",
    "aku" -> "Alaska",
    "alu" -> "Alabama",
    "am " -> "Anguilla",
    "an " -> "Andorra",
    "ao " -> "Angola",
    "aq " -> "Antigua and Barbuda",
    "aru" -> "Arkansas",
    "as " -> "American Samoa",
    "at " -> "Australia",
    "au " -> "Austria",
    "aw " -> "Aruba",
    "ay " -> "Antarctica",
    "azu" -> "Arizona",
    "ba " -> "Bahrain",
    "bb " -> "Barbados",
    "bcc" -> "British Columbia",
    "bd " -> "Burundi",
    "be " -> "Belgium",
    "bf " -> "Bahamas",
    "bg " -> "Bangladesh",
    "bh " -> "Belize",
    "bi " -> "British Indian Ocean Territory",
    "bl " -> "Brazil",
    "bm " -> "Bermuda Islands",
    "bn " -> "Bosnia and Herzegovina",
    "bo " -> "Bolivia",
    "bp " -> "Solomon Islands",
    "br " -> "Burma",
    "bs " -> "Botswana",
    "bt " -> "Bhutan",
    "bu " -> "Bulgaria",
    "bv " -> "Bouvet Island",
    "bw " -> "Belarus",
    "bwr" -> "Byelorussian S.S.R.",
    "bx " -> "Brunei",
    "ca " -> "Caribbean Netherlands",
    "cau" -> "California",
    "cb " -> "Cambodia",
    "cc " -> "China",
    "cd " -> "Chad",
    "ce " -> "Sri Lanka",
    "cf " -> "Congo (Brazzaville)",
    "cg " -> "Congo (Democratic Republic)",
    "ch " -> "China (Republic : 1949- )",
    "ci " -> "Croatia",
    "cj " -> "Cayman Islands",
    "ck " -> "Colombia",
    "cl " -> "Chile",
    "cm " -> "Cameroon",
    "cn " -> "Canada",
    "co " -> "Curaçao",
    "cou" -> "Colorado",
    "cp " -> "Canton and Enderbury Islands",
    "cq " -> "Comoros",
    "cr " -> "Costa Rica",
    "cs " -> "Czechoslovakia",
    "ctu" -> "Connecticut",
    "cu " -> "Cuba",
    "cv " -> "Cabo Verde",
    "cw " -> "Cook Islands",
    "cx " -> "Central African Republic",
    "cy " -> "Cyprus",
    "cz " -> "Canal Zone",
    "dcu" -> "District of Columbia",
    "deu" -> "Delaware",
    "dk " -> "Denmark",
    "dm " -> "Benin",
    "dq " -> "Dominica",
    "dr " -> "Dominican Republic",
    "ea " -> "Eritrea",
    "ec " -> "Ecuador",
    "eg " -> "Equatorial Guinea",
    "em " -> "Timor-Leste",
    "enk" -> "England",
    "er " -> "Estonia",
    "err" -> "Estonia",
    "es " -> "El Salvador",
    "et " -> "Ethiopia",
    "fa " -> "Faroe Islands",
    "fg " -> "French Guiana",
    "fi " -> "Finland",
    "fj " -> "Fiji",
    "fk " -> "Falkland Islands",
    "flu" -> "Florida",
    "fm " -> "Micronesia (Federated States)",
    "fp " -> "French Polynesia",
    "fr " -> "France",
    "fs " -> "Terres australes et antarctiques françaises",
    "ft " -> "Djibouti",
    "gau" -> "Georgia",
    "gb " -> "Kiribati",
    "gd " -> "Grenada",
    "ge " -> "Germany (East)",
    "gg " -> "Guernsey",
    "gh " -> "Ghana",
    "gi " -> "Gibraltar",
    "gl " -> "Greenland",
    "gm " -> "Gambia",
    "gn " -> "Gilbert and Ellice Islands",
    "go " -> "Gabon",
    "gp " -> "Guadeloupe",
    "gr " -> "Greece",
    "gs " -> "Georgia (Republic)",
    "gsr" -> "Georgian S.S.R.",
    "gt " -> "Guatemala",
    "gu " -> "Guam",
    "gv " -> "Guinea",
    "gw " -> "Germany",
    "gy " -> "Guyana",
    "gz " -> "Gaza Strip",
    "hiu" -> "Hawaii",
    "hk " -> "Hong Kong",
    "hm " -> "Heard and McDonald Islands",
    "ho " -> "Honduras",
    "ht " -> "Haiti",
    "hu " -> "Hungary",
    "iau" -> "Iowa",
    "ic " -> "Iceland",
    "idu" -> "Idaho",
    "ie " -> "Ireland",
    "ii " -> "India",
    "ilu" -> "Illinois",
    "im " -> "Isle of Man",
    "inu" -> "Indiana",
    "io " -> "Indonesia",
    "iq " -> "Iraq",
    "ir " -> "Iran",
    "is " -> "Israel",
    "it " -> "Italy",
    "iu " -> "Israel-Syria Demilitarized Zones",
    "iv " -> "Côte d'Ivoire",
    "iw " -> "Israel-Jordan Demilitarized Zones",
    "iy " -> "Iraq-Saudi Arabia Neutral Zone",
    "ja " -> "Japan",
    "je " -> "Jersey",
    "ji " -> "Johnston Atoll",
    "jm " -> "Jamaica",
    "jn " -> "Jan Mayen",
    "jo " -> "Jordan",
    "ke " -> "Kenya",
    "kg " -> "Kyrgyzstan",
    "kgr" -> "Kirghiz S.S.R.",
    "kn " -> "Korea (North)",
    "ko " -> "Korea (South)",
    "ksu" -> "Kansas",
    "ku " -> "Kuwait",
    "kv " -> "Kosovo",
    "kyu" -> "Kentucky",
    "kz " -> "Kazakhstan",
    "kzr" -> "Kazakh S.S.R.",
    "lau" -> "Louisiana",
    "lb " -> "Liberia",
    "le " -> "Lebanon",
    "lh " -> "Liechtenstein",
    "li " -> "Lithuania",
    "lir" -> "Lithuania",
    "ln " -> "Central and Southern Line Islands",
    "lo " -> "Lesotho",
    "ls " -> "Laos",
    "lu " -> "Luxembourg",
    "lv " -> "Latvia",
    "lvr" -> "Latvia",
    "ly " -> "Libya",
    "mau" -> "Massachusetts",
    "mbc" -> "Manitoba",
    "mc " -> "Monaco",
    "mdu" -> "Maryland",
    "meu" -> "Maine",
    "mf " -> "Mauritius",
    "mg " -> "Madagascar",
    "mh " -> "Macao",
    "miu" -> "Michigan",
    "mj " -> "Montserrat",
    "mk " -> "Oman",
    "ml " -> "Mali",
    "mm " -> "Malta",
    "mnu" -> "Minnesota",
    "mo " -> "Montenegro",
    "mou" -> "Missouri",
    "mp " -> "Mongolia",
    "mq " -> "Martinique",
    "mr " -> "Morocco",
    "msu" -> "Mississippi",
    "mtu" -> "Montana",
    "mu " -> "Mauritania",
    "mv " -> "Moldova",
    "mvr" -> "Moldavian S.S.R.",
    "mw " -> "Malawi",
    "mx " -> "Mexico",
    "my " -> "Malaysia",
    "mz " -> "Mozambique",
    "na " -> "Netherlands Antilles",
    "nbu" -> "Nebraska",
    "ncu" -> "North Carolina",
    "ndu" -> "North Dakota",
    "ne " -> "Netherlands",
    "nfc" -> "Newfoundland and Labrador",
    "ng " -> "Niger",
    "nhu" -> "New Hampshire",
    "nik" -> "Northern Ireland",
    "nju" -> "New Jersey",
    "nkc" -> "New Brunswick",
    "nl " -> "New Caledonia",
    "nm " -> "Northern Mariana Islands",
    "nmu" -> "New Mexico",
    "nn " -> "Vanuatu",
    "no " -> "Norway",
    "np " -> "Nepal",
    "nq " -> "Nicaragua",
    "nr " -> "Nigeria",
    "nsc" -> "Nova Scotia",
    "ntc" -> "Northwest Territories",
    "nu " -> "Nauru",
    "nuc" -> "Nunavut",
    "nvu" -> "Nevada",
    "nw " -> "Northern Mariana Islands",
    "nx " -> "Norfolk Island",
    "nyu" -> "New York (State)",
    "nz " -> "New Zealand",
    "ohu" -> "Ohio",
    "oku" -> "Oklahoma",
    "onc" -> "Ontario",
    "oru" -> "Oregon",
    "ot " -> "Mayotte",
    "pau" -> "Pennsylvania",
    "pc " -> "Pitcairn Island",
    "pe " -> "Peru",
    "pf " -> "Paracel Islands",
    "pg " -> "Guinea-Bissau",
    "ph " -> "Philippines",
    "pic" -> "Prince Edward Island",
    "pk " -> "Pakistan",
    "pl " -> "Poland",
    "pn " -> "Panama",
    "po " -> "Portugal",
    "pp " -> "Papua New Guinea",
    "pr " -> "Puerto Rico",
    "pt " -> "Portuguese Timor",
    "pw " -> "Palau",
    "py " -> "Paraguay",
    "qa " -> "Qatar",
    "qea" -> "Queensland",
    "quc" -> "Québec (Province)",
    "rb " -> "Serbia",
    "re " -> "Réunion",
    "rh " -> "Zimbabwe",
    "riu" -> "Rhode Island",
    "rm " -> "Romania",
    "ru " -> "Russia (Federation)",
    "rur" -> "Russian S.F.S.R.",
    "rw " -> "Rwanda",
    "ry " -> "Ryukyu Islands, Southern",
    "sa " -> "South Africa",
    "sb " -> "Svalbard",
    "sc " -> "Saint-Barthélemy",
    "scu" -> "South Carolina",
    "sd " -> "South Sudan",
    "sdu" -> "South Dakota",
    "se " -> "Seychelles",
    "sf " -> "Sao Tome and Principe",
    "sg " -> "Senegal",
    "sh " -> "Spanish North Africa",
    "si " -> "Singapore",
    "sj " -> "Sudan",
    "sk " -> "Sikkim",
    "sl " -> "Sierra Leone",
    "sm " -> "San Marino",
    "sn " -> "Sint Maarten",
    "snc" -> "Saskatchewan",
    "so " -> "Somalia",
    "sp " -> "Spain",
    "sq " -> "Eswatini",
    "sr " -> "Surinam",
    "ss " -> "Western Sahara",
    "st " -> "Saint-Martin",
    "stk" -> "Scotland",
    "su " -> "Saudi Arabia",
    "sv " -> "Swan Islands",
    "sw " -> "Sweden",
    "sx " -> "Namibia",
    "sy " -> "Syria",
    "sz " -> "Switzerland",
    "ta " -> "Tajikistan",
    "tar" -> "Tajik S.S.R.",
    "tc " -> "Turks and Caicos Islands",
    "tg " -> "Togo",
    "th " -> "Thailand",
    "ti " -> "Tunisia",
    "tk " -> "Turkmenistan",
    "tkr" -> "Turkmen S.S.R.",
    "tl " -> "Tokelau",
    "tma" -> "Tasmania",
    "tnu" -> "Tennessee",
    "to " -> "Tonga",
    "tr " -> "Trinidad and Tobago",
    "ts " -> "United Arab Emirates",
    "tt " -> "Trust Territory of the Pacific Islands",
    "tu " -> "Turkey",
    "tv " -> "Tuvalu",
    "txu" -> "Texas",
    "tz " -> "Tanzania",
    "ua " -> "Egypt",
    "uc " -> "United States Misc. Caribbean Islands",
    "ug " -> "Uganda",
    "ui " -> "United Kingdom Misc. Islands",
    "uik" -> "United Kingdom Misc. Islands",
    "uk " -> "United Kingdom",
    "un " -> "Ukraine",
    "unr" -> "Ukraine",
    "up " -> "United States Misc. Pacific Islands",
    "ur " -> "Soviet Union",
    "us " -> "United States",
    "utu" -> "Utah",
    "uv " -> "Burkina Faso",
    "uy " -> "Uruguay",
    "uz " -> "Uzbekistan",
    "uzr" -> "Uzbek S.S.R.",
    "vau" -> "Virginia",
    "vb " -> "British Virgin Islands",
    "vc " -> "Vatican City",
    "ve " -> "Venezuela",
    "vi " -> "Virgin Islands of the United States",
    "vm " -> "Vietnam",
    "vn " -> "Vietnam, North",
    "vp " -> "Various places",
    "vra" -> "Victoria",
    "vs " -> "Vietnam, South",
    "vtu" -> "Vermont",
    "wau" -> "Washington (State)",
    "wb " -> "West Berlin",
    "wea" -> "Western Australia",
    "wf " -> "Wallis and Futuna",
    "wiu" -> "Wisconsin",
    "wj " -> "West Bank of the Jordan River",
    "wk " -> "Wake Island",
    "wlk" -> "Wales",
    "ws " -> "Samoa",
    "wvu" -> "West Virginia",
    "wyu" -> "Wyoming",
    "xa " -> "Christmas Island (Indian Ocean)",
    "xb " -> "Cocos (Keeling) Islands",
    "xc " -> "Maldives",
    "xd " -> "Saint Kitts-Nevis",
    "xe " -> "Marshall Islands",
    "xf " -> "Midway Islands",
    "xga" -> "Coral Sea Islands Territory",
    "xh " -> "Niue",
    "xi " -> "Saint Kitts-Nevis-Anguilla",
    "xj " -> "Saint Helena",
    "xk " -> "Saint Lucia",
    "xl " -> "Saint Pierre and Miquelon",
    "xm " -> "Saint Vincent and the Grenadines",
    "xn " -> "North Macedonia",
    "xna" -> "New South Wales",
    "xo " -> "Slovakia",
    "xoa" -> "Northern Territory",
    "xp " -> "Spratly Island",
    "xr " -> "Czech Republic",
    "xra" -> "South Australia",
    "xs " -> "South Georgia and the South Sandwich Islands",
    "xv " -> "Slovenia",
    "xx " -> "No place, unknown, or undetermined",
    "xxc" -> "Canada",
    "xxk" -> "United Kingdom",
    "xxr" -> "Soviet Union",
    "xxu" -> "United States",
    "ye " -> "Yemen",
    "ykc" -> "Yukon Territory",
    "ys " -> "Yemen (People's Democratic Republic)",
    "yu " -> "Serbia and Montenegro",
    "za " -> "Zambia"
  )
}
