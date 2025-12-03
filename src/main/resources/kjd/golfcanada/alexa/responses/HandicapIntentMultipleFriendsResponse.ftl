{
    "outputSpeech": {
        "type": "PlainText",
        "text": "I found ${count} friends with the name '${query}'. <#list friends as friend><#if friend.name??>${friend.name}<#else>A friend</#if> has a handicap index of ${friend.handicap!"unavailable"}. </#list>"
    },
    "shouldEndSession": false
}
