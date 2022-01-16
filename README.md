# poeditor-proxy

This is a simple Kotlin based webserver that can proxy that has one job:

Proxy requests for translations files through to poeditor.com's export API.

## Configuration

There is not much to configure, the program takes settings from the following environment variables:

 * `POEDITOR_API_TOKEN` (required): The API token necessary to access the poeditor.com API
 * `POEDITOR_PROJECT_ID` (required): The ID of the translation project on poeditor.com
 * `FORCED_CONTENT_TYPE` (optional): Overwrites the response content type from the export download.
 * `ROOT_PATH` (optional): Configures a prefix for all routes. This can be used to host several instances of the proxy behind different path prefixes.

## Endpoints

All endpoints are exposed on port `8080`.

### `/export/{type}/{file}`

#### Arguments

 * `{type}`: file type of the file to be exported, check our [the documentation](https://poeditor.com/docs/api#projects_export)
 * `{file}`: the name of the requested file. Must be either `{language}.{ext}` or just `{language}`.

#### Examples

 * `/export/key_value_json/en-US.json`
 * `/export/po/en-US.po`

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
