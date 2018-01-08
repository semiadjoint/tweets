package com.example.tweetstat

case class Url(
    displayUrl: String
)

case class Message(
    text: String,
    urls: List[Url],
)
