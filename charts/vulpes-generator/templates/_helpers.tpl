{{/*
The helpers below are still named `micronaut.*`, not `vulpes-generator.*`, and
that is deliberate: this chart is a fork of helm/micronaut in the cluster
repository, and keeping the templates byte-identical is what lets a later fix
over there be carried across with `diff -r`. Renaming them would touch every
template file and turn that diff into noise.

The rendered resource names are unaffected either way -- they come from
.Chart.Name/.Release.Name, and the cluster overlays pin them with
nameOverride/fullnameOverride so the Deployment's immutable selector survives
this chart move.
*/}}

{{/*
Expand the name of the chart.
*/}}
{{- define "micronaut.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "micronaut.fullname" -}}
{{- if .Values.fullnameOverride }}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- if contains .Chart.Name .Release.Name }}
{{- .Release.Name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name .Chart.Name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}
{{- end }}

{{/*
Chart name and version
*/}}
{{- define "micronaut.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "micronaut.labels" -}}
app.kubernetes.io/name: {{ include "micronaut.name" . }}
helm.sh/chart: {{ include "micronaut.chart" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}


{{/*
Create the name of the service account to use
*/}}
{{- define "micronaut.serviceAccountName" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "micronaut.fullname" .) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}

{{/*
Selector labels
*/}}
{{- define "micronaut.selectorLabels" -}}
app.kubernetes.io/name: {{ include "micronaut.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}