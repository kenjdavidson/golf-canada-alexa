{
    "outputSpeech": {
        "type": "PlainText",
        "text": "<#if name??>${name}'s<#else>The player's</#if> current handicap index is ${handicap!"unavailable"}.<#if lowValue??> Their low handicap index is ${lowValue?string["0.0"]}.</#if>"
    },
    "shouldEndSession": "false"
}
