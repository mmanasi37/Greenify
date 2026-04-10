const { onCall, HttpsError } = require("firebase-functions/v2/https");
const { defineSecret } = require("firebase-functions/params");
const admin = require("firebase-admin");

admin.initializeApp();

const CLIMATIQ_API_KEY = defineSecret("CLIMATIQ_API_KEY");

const ELECTRICITY_ACTIVITY = {
  "air conditioner": "electricity-energy_source-grid_mix",
  "electric heater": "electricity-energy_source-grid_mix",
  "refrigerator": "electricity-energy_source-grid_mix",
  "washing machine": "electricity-energy_source-grid_mix",
  "dishwasher": "electricity-energy_source-grid_mix",
  "electric oven": "electricity-energy_source-grid_mix",
  "microwave": "electricity-energy_source-grid_mix",
  "desktop computer": "electricity-energy_source-grid_mix",
  "laptop": "electricity-energy_source-grid_mix",
  "television": "electricity-energy_source-grid_mix",
  "water pump": "electricity-energy_source-grid_mix",
  "lighting": "electricity-energy_source-grid_mix"
};

const FOOD_ACTIVITY = {
  beef: "food-type_beef",
  chicken: "food-type_chicken",
  pork: "food-type_pork",
  lamb: "food-type_lamb",
  fish: "food-type_fish",
  rice: "food-type_rice",
  milk: "food-type_milk",
  cheese: "food-type_cheese",
  eggs: "food-type_eggs",
  tofu: "food-type_tofu"
};

const TRANSPORT_ACTIVITY = {
  car: "passenger_vehicle-vehicle_type_car-fuel_source_petrol",
  bus: "passenger_vehicle-vehicle_type_bus-fuel_source_diesel",
  train: "passenger_train-route_type_national_rail",
  motorbike: "passenger_vehicle-vehicle_type_motorbike-fuel_source_petrol",
  bicycle: "passenger_vehicle-vehicle_type_bicycle",
  walking: "passenger_vehicle-vehicle_type_walk",
  taxi: "passenger_vehicle-vehicle_type_taxi-fuel_source_petrol",
  truck: "freight_vehicle-vehicle_type_truck-fuel_source_diesel",
  ferry: "ferry-route_type_domestic",
  airplane: "passenger_flight-route_type_short_haul"
};

const ELECTRICITY_FALLBACK = {
  "air conditioner": 1200,
  "electric heater": 1800,
  "refrigerator": 180,
  "washing machine": 500,
  "dishwasher": 1200,
  "electric oven": 2000,
  "microwave": 1100,
  "desktop computer": 200,
  laptop: 60,
  television: 100,
  "water pump": 750,
  lighting: 60
};

const FOOD_FALLBACK = {
  beef: 27,
  chicken: 6.9,
  pork: 12.1,
  lamb: 39.2,
  fish: 6.0,
  rice: 2.7,
  milk: 1.9,
  cheese: 13.5,
  eggs: 4.8,
  tofu: 2.0
};

const TRANSPORT_FALLBACK = {
  car: 0.192,
  bus: 0.105,
  train: 0.041,
  motorbike: 0.103,
  bicycle: 0,
  walking: 0,
  taxi: 0.192,
  truck: 0.27,
  ferry: 0.115,
  airplane: 0.255
};

function toNumber(value, name) {
  const n = Number(value);
  if (!Number.isFinite(n) || n < 0) {
    throw new HttpsError("invalid-argument", `${name} must be a valid non-negative number`);
  }
  return n;
}

function normalizeText(value, name) {
  const text = (value || "").toString().trim().toLowerCase();
  if (!text) {
    throw new HttpsError("invalid-argument", `${name} is required`);
  }
  return text;
}

async function callClimatiq({ apiKey, activityId, parameters }) {
  const response = await fetch("https://api.climatiq.io/data/v1/estimate", {
    method: "POST",
    headers: {
      Authorization: `Bearer ${apiKey}`,
      "Content-Type": "application/json"
    },
    body: JSON.stringify({
      emission_factor: { activity_id: activityId },
      parameters
    })
  });

  if (!response.ok) {
    const body = await response.text();
    throw new Error(`Climatiq API error (${response.status}): ${body}`);
  }

  const data = await response.json();
  const kgCO2e = Number(data.co2e);
  if (!Number.isFinite(kgCO2e)) {
    throw new Error("Invalid Climatiq response");
  }

  return kgCO2e;
}

exports.estimateElectricityEmissions = onCall({ secrets: [CLIMATIQ_API_KEY] }, async (request) => {
  const appliance = normalizeText(request.data?.appliance, "appliance");
  const hours = toNumber(request.data?.hours, "hours");

  const activityId = ELECTRICITY_ACTIVITY[appliance] || "electricity-energy_source-grid_mix";

  try {
    const kgCO2e = await callClimatiq({
      apiKey: CLIMATIQ_API_KEY.value(),
      activityId,
      parameters: {
        energy: hours,
        energy_unit: "kWh"
      }
    });

    return { kg_co2e: kgCO2e, source: "climatiq" };
  } catch (err) {
    const factorWh = ELECTRICITY_FALLBACK[appliance] ?? 500;
    const kgCO2e = (hours * factorWh) / 1_000_000;
    return { kg_co2e: kgCO2e, source: "fallback" };
  }
});

exports.estimateFoodEmissions = onCall({ secrets: [CLIMATIQ_API_KEY] }, async (request) => {
  const food = normalizeText(request.data?.food, "food");
  const amount = toNumber(request.data?.amount, "amount");

  const activityId = FOOD_ACTIVITY[food] || "food-type_beef";

  try {
    const kgCO2e = await callClimatiq({
      apiKey: CLIMATIQ_API_KEY.value(),
      activityId,
      parameters: {
        weight: amount,
        weight_unit: "kg"
      }
    });

    return { kg_co2e: kgCO2e, source: "climatiq" };
  } catch (err) {
    const factor = FOOD_FALLBACK[food] ?? 6.0;
    return { kg_co2e: amount * factor, source: "fallback" };
  }
});

exports.estimateTransportEmissions = onCall({ secrets: [CLIMATIQ_API_KEY] }, async (request) => {
  const transport = normalizeText(request.data?.transport, "transport");
  const distance = toNumber(request.data?.distance, "distance");

  const activityId = TRANSPORT_ACTIVITY[transport] || "passenger_vehicle-vehicle_type_car-fuel_source_petrol";

  try {
    const kgCO2e = await callClimatiq({
      apiKey: CLIMATIQ_API_KEY.value(),
      activityId,
      parameters: {
        distance,
        distance_unit: "km"
      }
    });

    return { kg_co2e: kgCO2e, source: "climatiq" };
  } catch (err) {
    const factor = TRANSPORT_FALLBACK[transport] ?? 0.192;
    return { kg_co2e: distance * factor, source: "fallback" };
  }
});
