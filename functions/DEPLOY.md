# Firebase Functions Deploy Guide

## 1. Prerequisites
- Install Firebase CLI: `npm install -g firebase-tools`
- Login: `firebase login`
- Select your project: `firebase use --add`

## 2. Install Dependencies
Run from repo root:

```bash
cd functions
npm install
```

## 3. Set Climatiq Secret
From repo root:

```bash
firebase functions:secrets:set CLIMATIQ_API_KEY
```

Paste your Climatiq API key when prompted.

## 4. Deploy Functions
From repo root:

```bash
firebase deploy --only functions
```

## 5. Callable Functions Exposed
- `estimateElectricityEmissions`
- `estimateFoodEmissions`
- `estimateTransportEmissions`

All return payload format:

```json
{
  "kg_co2e": 1.23,
  "source": "climatiq"
}
```

If Climatiq is unreachable, each endpoint automatically returns a `fallback` estimate.
