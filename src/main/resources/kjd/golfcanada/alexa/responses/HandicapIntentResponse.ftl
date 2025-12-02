{
    "outputSpeech": {
        "type": "PlainText",
        "text": "<#if name??>Hello ${name}. </#if>Your current handicap index is ${handicap!"unavailable"}.<#if lowValue??> Your low handicap index is ${lowValue?string["0.0"]}.</#if><#if averageDifferential??> Your average differential is ${averageDifferential?string["0.0"]}.</#if>"
    },
    "shouldEndSession": false
}
