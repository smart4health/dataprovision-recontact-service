# Smart4Health Data-provision Recontact Service

## Acknowledgements

<img src="./img/eu.jpg" align="left" alt="European Flag" width="60">

This project has received funding from the European Union’s Horizon 2020 research and innovation programme under grant agreement No 826117.

## About

After citizens have provided their data anonymously via
the [MyScience App](https://github.com/smart4health/my-science-app), they can opt in to receiving re-contact messages
from researchers of the platform. This service manages communicates with a Jira instance which serves as Data Access
Committee Frontend service and then delivers the messages with a pull mechanism to the app.

## Introduction

This service manages the contact between the [MyScience App](https://github.com/smart4health/my-science-app) and the
researchers who assemble a cohort of citizens they want to contact.

They will receive an encrypted cohort info file when creating this cohort in separate frontend service. In a designated
Jira system they will create a new issue for this communication `Request` together with this file as attachment. Through
a webhook the
recontact-service will receive an update that an attachment has been uploaded. It will fetch the file, decrypt it and
post the relevant metadata contents in a custom field in this Jira issue.

A designated access committee approves or declines this request. Again a webhook will notify the recontact-service that
a request has been approved including the citizen message content and from this point in time it will be persisted in
the database including the creation of all citizen messages. The citizens (or better
their [MyScience App](https://github.com/smart4health/my-science-app)) will query those messages regularly, and also
send
a read receipt back to the service.

Each change to a request will trigger an update to a custom report field in the Jira issue.

Requests can also be cancelled. This leads to deletion of the created messages.

Example flow through the REST services:

1. Approved request enters the service: `POST /v1/requests`<sup>[1](#creation)</sup>. After this for each citizen
   a `Message` has been created in the database. The body only contains the requestId and the citizen message content.
   All other information are being fetched out from the Jira.
2. Users query on a regular basis if they have unread messages via
   the [MyScience App](https://github.com/smart4health/my-science-app): `GET /v1/messages?state=created`<sup>[2](#checkmessages)</sup>
   .
3. The app sends a read receipt for tracking purposes: `POST /v1/message/{messageId}`<sup>[3](#readreceipt)</sup>.

For other endpoints include Get Message<sup>[4](#getmessage)</sup>, Get Request<sup>[5](#getrequest)</sup>, Create
Report<sup>[6](#report)</sup>
Delete Request<sup>[7](#deleterequest)</sup>.

## Local Deployment

### Postgres

> Note: A postgres connection is required for the service to run

For local development, start a postgres container so:

```shell
docker run --rm -it --name postgres -e POSTGRES_PASSWORD=password -e POSTGRES_USER=username -e POSTGRES_DB=development -p 5432:5432 postgres
```

To use `psql` on this database, run:

```shell
docker exec -it postgres psql -U username -d development
```

> Note: `DatabaseConfiguration` sets the `spring.datasource.url` property manually
> from aws secrets (or mock-secrets in ENV vars if secrets profile is not used)
> `jdbc:` is then added manually

### Application start

After postgres is set up, start the application from root dir: `./gradlew bootRun`.

By default, Jira connection is mocked. For features requiring a real connection to the Jira instance, use spring
profile `jira` to start the application (usually not the case) and manually inject the api credentials:
`SPRING_PROFILES_ACTIVE=jira MOCKSECRETS_JIRACREDENTIALS='{"username":"","password":""}' ./gradlew bootRun`

If those api credentials should be fetched from aws secrets, you may
use: `SPRING_PROFILES_ACTIVE=jira,secrets-aws ./gradlew bootRun`

## Decryption and Encryption

The research platform is encrypting their cohort files symmetrically with ChaCha20-Poly1305-IETF. To simulate this, use
the tasks:

```shell
./gradlew encryptFileTask
./gradlew decryptFileTask
```

with optional parameters `inputFilePath`, `outputFileName` and `key` for the hex encoded shared secret.

## Hashicorp Vault

To run Vault locally and let recontact use approle authentication using sample secrets, proceed the following:

1. Start Vault dev server locally (dev mode skips unsealing and some defaults):

```shell
docker run --rm -p 8200:8200 --cap-add=IPC_LOCK --name=vault-dev -e 'VAULT_DEV_ROOT_TOKEN_ID=root' vault
```

2. Run this custom set up script for the initial token, policies, roles and secrets. Also starts the service which will
   use the initial token to renew the approle lease every 20 seconds and using full pull mode to fetch the role-id and
   secret-id:

```shell
SPRING_CLOUD_VAULT_TOKEN=$(sh vault_local.sh | tail -1) SPRING_PROFILES_ACTIVE=secrets-vault ./gradlew bootRun
```

## Appendix: curl examples

### Creation

```shell
curl --location --request POST 'http://localhost:8080/v1/requests' \
--header 'Content-Type: application/json' \
--data-raw '{
    "id": "external-jira-ticket-id",
    "message": "Hello!",
    "title": "Title"
}'
```

### CheckMessages

```shell
curl --location --request GET 'http://localhost:8080/v1/messages?state=created' \
--header 'X-Recontact-Citizen-Id: 161c602c-9e97-11eb-a8b3-0242ac130001'
```

### GetRequest

```shell
curl --location --request GET 'http://localhost:8080/v1/request/some-external-id'
```

### GetMessage

```shell
curl --location --request GET 'http://localhost:8080/v1/message/57419034-5159-42f2-8b68-dd60f75fd838' \
--header 'X-Recontact-Citizen-Id: 161c602c-9e97-11eb-a8b3-0242ac130003'
```

### ReadReceipt

```shell
curl --location --request POST 'http://localhost:8080/v1/message/57419034-5159-42f2-8b68-dd60f75fd838' \
--header 'X-Recontact-Citizen-Id: 161c602c-9e97-11eb-a8b3-0242ac130003' \
--header 'Content-Type: application/json' \
--data-raw '{
    "action": "read"
}'
```

### Report

```shell
curl --location --request GET 'http://localhost:8080/v1/reports'
```

### DeleteRequest

```shell
curl --location --request DELETE 'http://localhost:8080/v1/request/some-external-id'
```

### Manually Insert Messages (debugging)

Use `debug` profile and inject using:

```shell
curl --location --request POST 'http://localhost:8080/v1/debug/messages' \
--header 'Content-Type: application/json' \
--data-raw '{
    "id": "00000000-0000-0000-0000-000000000000",
    "linkedRequest": "this-does-not-matter",
    "createdAt": "2021-06-17T10:49:34.84Z",
    "updatedAt": null,
    "content": {
        "text": "Hello,\r\nBest,\r\nResearcher",
        "title": "Title"
    },
    "recipientId": "the-user-secret-you-want-to-use",
    "state": "CREATED"
}'
```

Confirm that it worked:

```shell
curl --location --request GET 'http://localhost:8080/v1/messages?state=created' \
--header 'X-Recontact-Citizen-Id: the-user-secret-you-want-to-use'
```