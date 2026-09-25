---
title: Edge Runtimes
description: Connect containerized embedded integration software running in your network to DocuDevs.
sidebar_position: 4
---

# Edge Runtimes

Edge runtimes is an integration feature where a containerized embedded integration software (based on Kestra) runs in your network and connects to DocuDevs over HTTPS: it registers with the control plane, then holds open a Server-Sent Events (SSE) connection to receive commands, and posts execution events back over plain HTTPS. There is no MQTT broker or client involved.

This allows you to securely process documents and integrate with internal systems without exposing them to the public internet.

## Overview

- **Secure Connection**: The runtime registers with DocuDevs using your API key, then keeps an SSE command stream open and reports execution events back over HTTPS.
- **Multi-tenancy**: Connections are securely isolated to your organization.
- **Monitoring**: You can view all connected edge runtimes and their execution statistics in the DocuDevs UI under **Integrations > Runtimes**.

## Setup

*Documentation for setting up and configuring Edge Runtimes is coming soon.*
