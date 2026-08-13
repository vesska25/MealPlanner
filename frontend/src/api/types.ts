export type AgentScenario = 'PANTRY_ASSISTANT' | 'MEAL_PLANNING' | 'SHOPPING_LIST'

export type RejectionReason =
  | 'DISLIKE_DISH'
  | 'NOT_TODAY'
  | 'TAKES_TOO_LONG'
  | 'DONT_WANT_CATEGORY'
  | 'TIRED_OF_INGREDIENT'

export type DiscardReason = 'EXPIRED_EARLY' | 'DIDNT_COOK_IN_TIME' | 'BOUGHT_TOO_MUCH'

export type AgentRunStatus =
  | 'RUNNING'
  | 'FINAL_RESPONSE'
  | 'TOOL_ERROR_FATAL'
  | 'PERMISSION_DENIED'
  | 'VALIDATION_FAILED'
  | 'ITERATION_LIMIT'
  | 'LLM_TIMEOUT'
  | 'BUDGET_EXCEEDED'
  | 'FALLBACK_RESPONSE'

export interface AuthResponse {
  token: string
}

export interface RegisterRequest {
  email: string
  password: string
  inviteCode: string
}

export interface LoginRequest {
  email: string
  password: string
}

export interface PantryItemResponse {
  id: number
  productName: string
  quantity: number
  unit: string
  purchasedAt: string
  expiresAt: string
  estimated: boolean
}

export interface ChatRequest {
  message: string
}

export interface ChatResponse {
  success: boolean
  status: AgentRunStatus
  message: string
}

export interface RecipeIngredientResponse {
  productName: string
  quantity: number
  unit: string
}

export interface ActiveSuggestionResponse {
  suggestionId: number
  recipeId: number
  recipeName: string
  cookTimeMinutes: number
  basePortions: number
  requiredEquipment: string[]
  ingredients: RecipeIngredientResponse[]
  score: number
}

export interface ConfirmCookingRequest {
  recipeId: number
  actualPortions: number
  idempotencyKey: string
}

export interface ConsumePortionsRequest {
  portionsEaten: number
}

export interface CookedDishResponse {
  id: number
  recipeId: number
  recipeName: string
  category: string
  totalPortions: number
  portionsRemaining: number
  kcalPerPortion: number | null
  proteinPerPortion: number | null
  fatPerPortion: number | null
  carbsPerPortion: number | null
  cookedAt: string
  expiresAt: string
  status: string
}

export interface MissingIngredientResponse {
  productId: number
  unit: string
  needed: number
  available: number
}

export interface CookingInfeasibleResponse {
  message: string
  missingIngredients: MissingIngredientResponse[]
}

export interface PantryItemExport {
  id: number
  productName: string
  quantity: number
  unit: string
  purchasedAt: string
  expiresAt: string
  status: string
  discardReason: string | null
}

export interface RecipeIngredientExport {
  productName: string
  quantity: number
  unit: string
}

export interface RecipeExport {
  id: number
  name: string
  cookTimeMinutes: number
  basePortions: number
  requiredEquipment: string[]
  ingredients: RecipeIngredientExport[]
}

export interface CookedDishExport {
  id: number
  recipeId: number
  category: string
  totalPortions: number
  portionsRemaining: number
  kcalPerPortion: number | null
  proteinPerPortion: number | null
  fatPerPortion: number | null
  carbsPerPortion: number | null
  cookedAt: string
  expiresAt: string
  status: string
}

export interface AgentRunExport {
  id: number
  scenario: string
  trigger: string
  status: string
  iterationCount: number
  startedAt: string
  finishedAt: string | null
}

export interface UserProfileExport {
  householdSize: number
  maxCookTimeWeekdayMinutes: number
  excludedProductNames: string[]
  equipment: string[]
  freeDays: string[]
  goal: string | null
  weeklyBudget: number | null
  preferredStores: string | null
  country: string | null
  sex: string | null
  ageYears: number | null
  heightCm: number | null
  weightKg: number | null
  activityLevel: string | null
  goalsEnabled: boolean
}

export interface AccountExportResponse {
  email: string
  pantryItems: PantryItemExport[]
  recipes: RecipeExport[]
  cookedDishes: CookedDishExport[]
  agentRuns: AgentRunExport[]
  // Null when onboarding hasn't been completed yet.
  profile: UserProfileExport | null
}

export interface RejectSuggestionRequest {
  reason: RejectionReason
}

export interface DiscardPantryItemRequest {
  reason: DiscardReason
}

export interface GenerateLinkCodeResponse {
  code: string
  deepLink: string
  expiresAt: string
}

export interface TelegramLinkStatusResponse {
  linked: boolean
  telegramUserId: number | null
  linkedAt: string | null
}

export interface ShoppingListItemResponse {
  id: number
  productName: string
  quantity: number
  unit: string
  block: 'DEFINITELY_NEED' | 'CHECK_MAYBE_OUT'
  status: 'PENDING' | 'PURCHASED' | 'ALREADY_HAVE' | 'NOT_BUYING' | 'NOT_NEEDED'
}

export interface ShoppingListResponse {
  id: number
  createdAt: string
  items: ShoppingListItemResponse[]
}
