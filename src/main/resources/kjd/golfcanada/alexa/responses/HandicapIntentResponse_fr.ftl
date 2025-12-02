{
    "outputSpeech": {
        "type": "PlainText",
        "text": "<#if name??>Bonjour ${name}. </#if>Votre index de handicap actuel est ${handicap!"non disponible"}.<#if lowValue??> Votre index de handicap bas est ${lowValue?string["0.0"]}.</#if><#if averageDifferential??> Votre différentiel moyen est ${averageDifferential?string["0.0"]}.</#if>"
    },
    "shouldEndSession": false
}
