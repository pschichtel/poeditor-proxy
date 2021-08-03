# poeditor-proxy

This is a simple Kotlin based webserver that can proxy that has one job:

Proxy requests for translations files through to poeditor.com's export API.

## Configuration

There is not much to configure, the program takes settings from the following environment variables:

* `POEDITOR_API_TOKEN`: The API token necessary to access the poeditor.com API
* `POEDITOR_PROJECT_ID`: The ID of the translation project on poeditor.com

## Endpoints

* `/export/{type}/{language}*`

## API Limits

Straight from the [poeditor.com documentation](https://poeditor.com/docs/api_rates):

Paid accounts:

 * max 200 concurrent requests
 * max 200 requests per minute
 * max 3600 requests per hour
 * max 1 upload request per 10 seconds

Free accounts:

 * max 150 concurrent requests
 * max 200 requests per minute
 * max 3600 requests per hour
 * max 1 upload request per 20 seconds
