/* USER CODE BEGIN Header */
/**
  ******************************************************************************
  * @file           : main.c
  * @brief          : Main program body
  ******************************************************************************
  * @attention
  *
  * <h2><center>&copy; Copyright (c) 2020 STMicroelectronics.
  * All rights reserved.</center></h2>
  *
  * This software component is licensed by ST under BSD 3-Clause license,
  * the "License"; You may not use this file except in compliance with the
  * License. You may obtain a copy of the License at:
  *                        opensource.org/licenses/BSD-3-Clause
  *
  ******************************************************************************
  */
/* USER CODE END Header */

/* Includes ------------------------------------------------------------------*/
#include "main.h"

/* Private includes ----------------------------------------------------------*/
/* USER CODE BEGIN Includes */
#include "stdio.h"
#include "math.h"
/* USER CODE END Includes */

/* Private typedef -----------------------------------------------------------*/
/* USER CODE BEGIN PTD */

/* USER CODE END PTD */

/* Private define ------------------------------------------------------------*/
/* USER CODE BEGIN PD */

// 定义预分频后的频率
#define PRESCALER_FREQ 80000

// 用于串口输出到ZigBee的数组的大小
#define UART_ZB_BUF_SIZE 10
// 用于串口输出到Log的数组大小
#define UART_LOG_BUF_SIZE 128

// ZigBee 发送数据长度
#define ZB_SEND_DATA_LEN 8
// ZigBee 接收数据长度
#define ZB_RECEIVE_DATA_LEN 5

// 串口输出到ZigBee，DMA方式
#define sendToZBByDMA(...) HAL_UART_Transmit_DMA(&huart1,\
										(uint8_t*)uart_ZB_buf,\
										sprintf(uart_ZB_buf, __VA_ARGS__))
// 串口输出到ZigBee，IT方式
#define sendToZBByIT(...) HAL_UART_Transmit_IT(&huart1,\
										(uint8_t*)uart_ZB_buf,\
										sprintf(uart_ZB_buf, __VA_ARGS__))
// 串口输出到Log，DMA方式
#define sendToLogByDMA(...) HAL_UART_Transmit_DMA(&huart3,\
										(uint8_t*)uart_log_buf,\
										sprintf(uart_log_buf, __VA_ARGS__))
// 串口输出到Log，IT方式
#define sendToLogByIT(...) HAL_UART_Transmit_IT(&huart3,\
										(uint8_t*)uart_log_buf,\
										sprintf(uart_log_buf, __VA_ARGS__))
/* USER CODE END PD */

/* Private macro -------------------------------------------------------------*/
/* USER CODE BEGIN PM */

/* USER CODE END PM */

/* Private variables ---------------------------------------------------------*/
TIM_HandleTypeDef htim1;

UART_HandleTypeDef huart1;
UART_HandleTypeDef huart3;
DMA_HandleTypeDef hdma_usart1_rx;
DMA_HandleTypeDef hdma_usart1_tx;
DMA_HandleTypeDef hdma_usart3_rx;
DMA_HandleTypeDef hdma_usart3_tx;

/* USER CODE BEGIN PV */

// 设备地址
const char DEVICE_ADDRESS = 'd';
// 设备类型
const char DEVICE_TYPE = 'c';
// 配对码
const char DEVICE_PAIR = 'o';

const uint8_t FULL_CIRCLE = 52;

// 上次执行的命令代码
char cmd_idx = 0;

// 指示窗帘的状态
char status = 'a';

char sce[3] = {'b'};

// 用于输出指定数量的pwm波
uint32_t pwm_counter = 0;
// 用于指定当前输出的pwm的数量
uint32_t pwm_counter_idx = 0;
// 用于指示步进电机工作状态
uint8_t motor_on = 0;


// ZigBee接收到数据的标志
uint8_t is_ZB_rec = 0;
// ZigBee接收数据缓存
uint8_t ZB_rec_buf[ZB_RECEIVE_DATA_LEN];

// 用于串口输出到ZigBee的数组的大小
char uart_ZB_buf[UART_ZB_BUF_SIZE];
char zb_send[UART_ZB_BUF_SIZE];

// 用于串口输出到Log的数组
char uart_log_buf[UART_LOG_BUF_SIZE];

/* USER CODE END PV */

/* Private function prototypes -----------------------------------------------*/
void SystemClock_Config(void);
static void MX_GPIO_Init(void);
static void MX_DMA_Init(void);
static void MX_TIM1_Init(void);
static void MX_USART1_UART_Init(void);
static void MX_USART3_UART_Init(void);
/* USER CODE BEGIN PFP */
uint8_t stepper_motor_contoler(uint8_t enable, uint8_t direction, uint16_t distance, uint8_t frequence);
void copyMat(uint8_t *a, uint8_t *b, uint8_t len);

/* USER CODE END PFP */

/* Private user code ---------------------------------------------------------*/
/* USER CODE BEGIN 0 */

/**
 * 步进电机的驱动设置为4细分，即每800个波形电机转一圈，TIM1的频率为72MHz,Prescaler为900-1,则分频后频率为80KHz
 *
 * @Param: enable: 电机是否允许空转，取0时不允许，取1时允许
 * @Param: direction: 取0时正转，取1时反转
 * @Param: distance: 电机总共转的半圈数
 * @Param: frequence: 电机转动频率，可取1-100，为了准确最好取可以被100整除的数字, 1代表半圈
 *
 * @Return: 0-没走， 1-开始走
 */
uint8_t stepper_motor_contoler(uint8_t enable, uint8_t direction, uint16_t distance, uint8_t frequence){
	if(distance == 0){
		return 0;
	}
	motor_on = 1;
	pwm_counter_idx = 0;
	// 控制点击是否允许空转
	if(enable == 0){
		HAL_GPIO_WritePin(GPIOA, GPIO_PIN_3, GPIO_PIN_SET);
	}else{
		HAL_GPIO_WritePin(GPIOA, GPIO_PIN_3, GPIO_PIN_RESET);
	}
	// 控制电机正转或反转
	if(direction == 0){
		HAL_GPIO_WritePin(GPIOA, GPIO_PIN_4, GPIO_PIN_SET);
	}else{
		HAL_GPIO_WritePin(GPIOA, GPIO_PIN_4, GPIO_PIN_RESET);
	}
	uint8_t reg_freq = 0;
	// reg_freq = 100 / frequecne;
	reg_freq = 200 / frequence;
	// 控制步进电机的转速
	TIM1->ARR = reg_freq-1;
	TIM1->CCR2 = (uint8_t)(reg_freq * 0.5);

	// 由上述可知，计时器的频率为 80K/reg_freq，则步进电机共走的圈数为T*frequence，同时n = T * f = T * 80K / ( 100 / frequence)
	// 控制电机走的圈数
	pwm_counter = distance * 200;
	// pwm_counter = distance * 800;

	// 开启时钟中断
	if(HAL_OK != HAL_TIM_Base_Start_IT(&htim1))
		Error_Handler();

	// 开启电机的PWM输出
	if(HAL_OK != HAL_TIM_PWM_Start(&htim1, TIM_CHANNEL_2))
		Error_Handler();

	return 1;
}

// 复制数组
void copyMat(uint8_t *a, uint8_t *b, uint8_t len){
	for(int i = 0; i< len; i++){
		b[i] = a[i];
	}
}

/* USER CODE END 0 */

/**
  * @brief  The application entry point.
  * @retval int
  */
int main(void)
{
  /* USER CODE BEGIN 1 */

  /* USER CODE END 1 */
  

  /* MCU Configuration--------------------------------------------------------*/

  /* Reset of all peripherals, Initializes the Flash interface and the Systick. */
  HAL_Init();

  /* USER CODE BEGIN Init */

  /* USER CODE END Init */

  /* Configure the system clock */
  SystemClock_Config();

  /* USER CODE BEGIN SysInit */

  /* USER CODE END SysInit */

  /* Initialize all configured peripherals */
  MX_GPIO_Init();
  MX_DMA_Init();
  MX_TIM1_Init();
  MX_USART1_UART_Init();
  MX_USART3_UART_Init();
  /* USER CODE BEGIN 2 */

  // 串口1使用DMA方式循环接收数据
  HAL_UART_Receive_DMA(&huart1,\
		  	  	  	   ZB_rec_buf,\
  					   ZB_RECEIVE_DATA_LEN);

  sendToLogByDMA("init curtain\n");
  HAL_Delay(50);
  sendToLogByDMA("%c\n", DEVICE_ADDRESS);
  /* USER CODE END 2 */
 
 

  /* Infinite loop */
  /* USER CODE BEGIN WHILE */
  uint8_t zb_cache[ZB_RECEIVE_DATA_LEN] = {0};
  while (1)
  {
	  // ZigBee接收到指令
	  if(is_ZB_rec == 1){
		  // sendToLogByDMA("zb get data! %c, %c, %c, %c, %c\n", ZB_rec_buf[0], ZB_rec_buf[1], ZB_rec_buf[2], ZB_rec_buf[3], ZB_rec_buf[4]);
		  // HAL_Delay(50);
		  // 将指令复制到cache
	  	  copyMat(ZB_rec_buf, zb_cache, ZB_RECEIVE_DATA_LEN);
	  	  is_ZB_rec = 0;
	  	  // 若已近处理则抛弃
	  	  if(zb_cache[1] == cmd_idx){
	  		  continue;
	  	  }else{
	  		  cmd_idx = zb_cache[1];
			  // sendToLogByDMA("zb get data!\n");
			  // HAL_Delay(50);
	  		  // 处理指令，配对
	  		  if(zb_cache[2] == 'a'){
	  			  if((DEVICE_PAIR == zb_cache[3]) && (DEVICE_TYPE == zb_cache[4])){
	  				  zb_send[2] = 'T';
	  			  }else{
	  				  zb_send[2] = 'F';
	  			  }
	  			  zb_send[0] = DEVICE_ADDRESS;
	  			  zb_send[1] = cmd_idx;
	  			  zb_send[3] = '|';
				  sendToLogByDMA("zb send data!\n");
				  HAL_Delay(50);
	  			  sendToZBByDMA(zb_send);
	  		  // 处理指令，控制窗帘的状态
	  		  }else if(zb_cache[2] == 'o'){
	  			  uint8_t circle = 0;
	  			  uint8_t dir = 0;
	  			  // 全部打开
	  			  if(zb_cache[3] == 'a'){
	  				  dir = 0;
	  				  if(status == 'a'){
	  					  circle = 0;
	  				  }else if(status == 'c'){
	  					  circle = FULL_CIRCLE/2;
	  				  }else if(status == 'e'){
	  					  circle = FULL_CIRCLE;
	  				  }
	  				  status = 'a';
	  			  // 打开一半
	  			  }else if(zb_cache[3] == 'c'){
	  				  if(status == 'a'){
	  					  circle = FULL_CIRCLE/2;
	  					  dir = 1;
	  				  }else if(status == 'c'){
	  					  circle = 0;
	  					  dir = 0;
	  				  }else if(status == 'e'){
	  					  circle = FULL_CIRCLE/2;
	  					  dir = 0;
	  				  }
	  				  status = 'c';
	  			  // 全部关闭
	  			  }else if(zb_cache[3] == 'e'){
	  				  dir = 1;
	  				  if(status == 'a'){
	  					  circle = FULL_CIRCLE;
	  				  }else if(status == 'c'){
	  					  circle = FULL_CIRCLE/2;
	  				  }else if(status == 'e'){
	  					  circle = 0;
	  				  }
	  				  status = 'e';
	  			  }
	  			  stepper_motor_contoler(1, dir, circle, 1);
	  			  zb_send[0] = DEVICE_ADDRESS;
	  			  zb_send[1] = cmd_idx;
	  			  zb_send[2] = 'T';
	  			  zb_send[3] = '|';
	  			  sendToZBByDMA(zb_send);
	  			  HAL_Delay(100);
	  			  // sendToLogByDMA("montor go %d, %d, %s\n", dir, circle, zb_send);
	  		  // 处理指令，返回窗帘的状态
	  		  }else if(zb_cache[2] == 'p'){
	  			  zb_send[0] = DEVICE_ADDRESS;
	  			  zb_send[1] = cmd_idx;
	  			  zb_send[2] = status;
	  			  zb_send[3] = '|';
	  			  sendToZBByDMA(zb_send);
	  		  // 处理指令，控制窗帘的场景
	  		  }else if(zb_cache[2] == 'q'){
	  			  if((zb_cache[3] == 'z') && (zb_cache[4] == 'c')){
		  			  zb_send[0] = DEVICE_ADDRESS;
		  			  zb_send[1] = cmd_idx;
		  			  zb_send[2] = sce[0];
		  			  zb_send[3] = sce[1];
		  			  zb_send[4] = sce[2];
		  			  zb_send[5] = '|';
		  			  sendToZBByDMA(zb_send);
	  			  }else{
	  				  if(zb_cache[3] == 'a'){
	  					  sce[0] = zb_cache[4];
	  				  }else if(zb_cache[3] == 'b'){
	  					  sce[1] = zb_cache[4];
	  				  }else if(zb_cache[3] == 'c'){
	  					  sce[2] = zb_cache[4];
	  				  }
		  			  zb_send[0] = DEVICE_ADDRESS;
		  			  zb_send[1] = cmd_idx;
		  			  zb_send[2] = 'T';
		  			  zb_send[3] = '|';
		  			  sendToZBByDMA(zb_send);
	  			  }
	  		  }
	  	  }
	  }
    /* USER CODE END WHILE */

    /* USER CODE BEGIN 3 */
  }
  /* USER CODE END 3 */
}

/**
  * @brief System Clock Configuration
  * @retval None
  */
void SystemClock_Config(void)
{
  RCC_OscInitTypeDef RCC_OscInitStruct = {0};
  RCC_ClkInitTypeDef RCC_ClkInitStruct = {0};

  /** Initializes the CPU, AHB and APB busses clocks 
  */
  RCC_OscInitStruct.OscillatorType = RCC_OSCILLATORTYPE_HSE;
  RCC_OscInitStruct.HSEState = RCC_HSE_ON;
  RCC_OscInitStruct.HSEPredivValue = RCC_HSE_PREDIV_DIV1;
  RCC_OscInitStruct.HSIState = RCC_HSI_ON;
  RCC_OscInitStruct.PLL.PLLState = RCC_PLL_ON;
  RCC_OscInitStruct.PLL.PLLSource = RCC_PLLSOURCE_HSE;
  RCC_OscInitStruct.PLL.PLLMUL = RCC_PLL_MUL9;
  if (HAL_RCC_OscConfig(&RCC_OscInitStruct) != HAL_OK)
  {
    Error_Handler();
  }
  /** Initializes the CPU, AHB and APB busses clocks 
  */
  RCC_ClkInitStruct.ClockType = RCC_CLOCKTYPE_HCLK|RCC_CLOCKTYPE_SYSCLK
                              |RCC_CLOCKTYPE_PCLK1|RCC_CLOCKTYPE_PCLK2;
  RCC_ClkInitStruct.SYSCLKSource = RCC_SYSCLKSOURCE_PLLCLK;
  RCC_ClkInitStruct.AHBCLKDivider = RCC_SYSCLK_DIV1;
  RCC_ClkInitStruct.APB1CLKDivider = RCC_HCLK_DIV2;
  RCC_ClkInitStruct.APB2CLKDivider = RCC_HCLK_DIV1;

  if (HAL_RCC_ClockConfig(&RCC_ClkInitStruct, FLASH_LATENCY_2) != HAL_OK)
  {
    Error_Handler();
  }
}

/**
  * @brief TIM1 Initialization Function
  * @param None
  * @retval None
  */
static void MX_TIM1_Init(void)
{

  /* USER CODE BEGIN TIM1_Init 0 */

  /* USER CODE END TIM1_Init 0 */

  TIM_ClockConfigTypeDef sClockSourceConfig = {0};
  TIM_MasterConfigTypeDef sMasterConfig = {0};
  TIM_OC_InitTypeDef sConfigOC = {0};
  TIM_BreakDeadTimeConfigTypeDef sBreakDeadTimeConfig = {0};

  /* USER CODE BEGIN TIM1_Init 1 */

  /* USER CODE END TIM1_Init 1 */
  htim1.Instance = TIM1;
  htim1.Init.Prescaler = 900-1;
  htim1.Init.CounterMode = TIM_COUNTERMODE_UP;
  htim1.Init.Period = 100-1;
  htim1.Init.ClockDivision = TIM_CLOCKDIVISION_DIV1;
  htim1.Init.RepetitionCounter = 0;
  htim1.Init.AutoReloadPreload = TIM_AUTORELOAD_PRELOAD_DISABLE;
  if (HAL_TIM_Base_Init(&htim1) != HAL_OK)
  {
    Error_Handler();
  }
  sClockSourceConfig.ClockSource = TIM_CLOCKSOURCE_INTERNAL;
  if (HAL_TIM_ConfigClockSource(&htim1, &sClockSourceConfig) != HAL_OK)
  {
    Error_Handler();
  }
  if (HAL_TIM_PWM_Init(&htim1) != HAL_OK)
  {
    Error_Handler();
  }
  sMasterConfig.MasterOutputTrigger = TIM_TRGO_RESET;
  sMasterConfig.MasterSlaveMode = TIM_MASTERSLAVEMODE_DISABLE;
  if (HAL_TIMEx_MasterConfigSynchronization(&htim1, &sMasterConfig) != HAL_OK)
  {
    Error_Handler();
  }
  sConfigOC.OCMode = TIM_OCMODE_PWM1;
  sConfigOC.Pulse = 0;
  sConfigOC.OCPolarity = TIM_OCPOLARITY_HIGH;
  sConfigOC.OCNPolarity = TIM_OCNPOLARITY_HIGH;
  sConfigOC.OCFastMode = TIM_OCFAST_DISABLE;
  sConfigOC.OCIdleState = TIM_OCIDLESTATE_RESET;
  sConfigOC.OCNIdleState = TIM_OCNIDLESTATE_RESET;
  if (HAL_TIM_PWM_ConfigChannel(&htim1, &sConfigOC, TIM_CHANNEL_2) != HAL_OK)
  {
    Error_Handler();
  }
  sBreakDeadTimeConfig.OffStateRunMode = TIM_OSSR_DISABLE;
  sBreakDeadTimeConfig.OffStateIDLEMode = TIM_OSSI_DISABLE;
  sBreakDeadTimeConfig.LockLevel = TIM_LOCKLEVEL_OFF;
  sBreakDeadTimeConfig.DeadTime = 0;
  sBreakDeadTimeConfig.BreakState = TIM_BREAK_DISABLE;
  sBreakDeadTimeConfig.BreakPolarity = TIM_BREAKPOLARITY_HIGH;
  sBreakDeadTimeConfig.AutomaticOutput = TIM_AUTOMATICOUTPUT_DISABLE;
  if (HAL_TIMEx_ConfigBreakDeadTime(&htim1, &sBreakDeadTimeConfig) != HAL_OK)
  {
    Error_Handler();
  }
  /* USER CODE BEGIN TIM1_Init 2 */

  /* USER CODE END TIM1_Init 2 */
  HAL_TIM_MspPostInit(&htim1);

}

/**
  * @brief USART1 Initialization Function
  * @param None
  * @retval None
  */
static void MX_USART1_UART_Init(void)
{

  /* USER CODE BEGIN USART1_Init 0 */

  /* USER CODE END USART1_Init 0 */

  /* USER CODE BEGIN USART1_Init 1 */

  /* USER CODE END USART1_Init 1 */
  huart1.Instance = USART1;
  huart1.Init.BaudRate = 115200;
  huart1.Init.WordLength = UART_WORDLENGTH_8B;
  huart1.Init.StopBits = UART_STOPBITS_1;
  huart1.Init.Parity = UART_PARITY_NONE;
  huart1.Init.Mode = UART_MODE_TX_RX;
  huart1.Init.HwFlowCtl = UART_HWCONTROL_NONE;
  huart1.Init.OverSampling = UART_OVERSAMPLING_16;
  if (HAL_UART_Init(&huart1) != HAL_OK)
  {
    Error_Handler();
  }
  /* USER CODE BEGIN USART1_Init 2 */

  /* USER CODE END USART1_Init 2 */

}

/**
  * @brief USART3 Initialization Function
  * @param None
  * @retval None
  */
static void MX_USART3_UART_Init(void)
{

  /* USER CODE BEGIN USART3_Init 0 */

  /* USER CODE END USART3_Init 0 */

  /* USER CODE BEGIN USART3_Init 1 */

  /* USER CODE END USART3_Init 1 */
  huart3.Instance = USART3;
  huart3.Init.BaudRate = 115200;
  huart3.Init.WordLength = UART_WORDLENGTH_8B;
  huart3.Init.StopBits = UART_STOPBITS_1;
  huart3.Init.Parity = UART_PARITY_NONE;
  huart3.Init.Mode = UART_MODE_TX_RX;
  huart3.Init.HwFlowCtl = UART_HWCONTROL_NONE;
  huart3.Init.OverSampling = UART_OVERSAMPLING_16;
  if (HAL_UART_Init(&huart3) != HAL_OK)
  {
    Error_Handler();
  }
  /* USER CODE BEGIN USART3_Init 2 */

  /* USER CODE END USART3_Init 2 */

}

/** 
  * Enable DMA controller clock
  */
static void MX_DMA_Init(void) 
{

  /* DMA controller clock enable */
  __HAL_RCC_DMA1_CLK_ENABLE();

  /* DMA interrupt init */
  /* DMA1_Channel2_IRQn interrupt configuration */
  HAL_NVIC_SetPriority(DMA1_Channel2_IRQn, 0, 0);
  HAL_NVIC_EnableIRQ(DMA1_Channel2_IRQn);
  /* DMA1_Channel3_IRQn interrupt configuration */
  HAL_NVIC_SetPriority(DMA1_Channel3_IRQn, 0, 0);
  HAL_NVIC_EnableIRQ(DMA1_Channel3_IRQn);
  /* DMA1_Channel4_IRQn interrupt configuration */
  HAL_NVIC_SetPriority(DMA1_Channel4_IRQn, 0, 0);
  HAL_NVIC_EnableIRQ(DMA1_Channel4_IRQn);
  /* DMA1_Channel5_IRQn interrupt configuration */
  HAL_NVIC_SetPriority(DMA1_Channel5_IRQn, 0, 0);
  HAL_NVIC_EnableIRQ(DMA1_Channel5_IRQn);

}

/**
  * @brief GPIO Initialization Function
  * @param None
  * @retval None
  */
static void MX_GPIO_Init(void)
{
  GPIO_InitTypeDef GPIO_InitStruct = {0};

  /* GPIO Ports Clock Enable */
  __HAL_RCC_GPIOD_CLK_ENABLE();
  __HAL_RCC_GPIOA_CLK_ENABLE();
  __HAL_RCC_GPIOB_CLK_ENABLE();

  /*Configure GPIO pin Output Level */
  HAL_GPIO_WritePin(GPIOA, GPIO_PIN_3|GPIO_PIN_4, GPIO_PIN_RESET);

  /*Configure GPIO pin Output Level */
  HAL_GPIO_WritePin(GPIOB, GPIO_PIN_3|GPIO_PIN_4, GPIO_PIN_SET);

  /*Configure GPIO pins : PA3 PA4 */
  GPIO_InitStruct.Pin = GPIO_PIN_3|GPIO_PIN_4;
  GPIO_InitStruct.Mode = GPIO_MODE_OUTPUT_PP;
  GPIO_InitStruct.Pull = GPIO_NOPULL;
  GPIO_InitStruct.Speed = GPIO_SPEED_FREQ_LOW;
  HAL_GPIO_Init(GPIOA, &GPIO_InitStruct);

  /*Configure GPIO pins : PB3 PB4 */
  GPIO_InitStruct.Pin = GPIO_PIN_3|GPIO_PIN_4;
  GPIO_InitStruct.Mode = GPIO_MODE_OUTPUT_PP;
  GPIO_InitStruct.Pull = GPIO_NOPULL;
  GPIO_InitStruct.Speed = GPIO_SPEED_FREQ_LOW;
  HAL_GPIO_Init(GPIOB, &GPIO_InitStruct);

}

/* USER CODE BEGIN 4 */
// 定时器中断回调函数
void HAL_TIM_PeriodElapsedCallback(TIM_HandleTypeDef *htim){
	if(htim->Instance == TIM1){
		if(motor_on == 1){
			if(pwm_counter_idx < pwm_counter){
				pwm_counter_idx ++;
			}else{
				motor_on = 0;

				// 关闭电机的PWM输出
				if(HAL_OK != HAL_TIM_PWM_Stop(&htim1, TIM_CHANNEL_2))
					Error_Handler();

				// 关闭时钟中断
				if(HAL_OK != HAL_TIM_Base_Stop_IT(&htim1))
					Error_Handler();
			}
		}
	}
}

// 串口1收到数据的回调函数
void HAL_UART_RxCpltCallback(UART_HandleTypeDef *huart){
	if (huart->Instance == USART1){
		// 若接收方为自己则接收

		if(ZB_rec_buf[0] == DEVICE_ADDRESS){
			is_ZB_rec = 1;
		}
	}
}


/* USER CODE END 4 */

/**
  * @brief  This function is executed in case of error occurrence.
  * @retval None
  */
void Error_Handler(void)
{
  /* USER CODE BEGIN Error_Handler_Debug */
  /* User can add his own implementation to report the HAL error return state */

  /* USER CODE END Error_Handler_Debug */
}

#ifdef  USE_FULL_ASSERT
/**
  * @brief  Reports the name of the source file and the source line number
  *         where the assert_param error has occurred.
  * @param  file: pointer to the source file name
  * @param  line: assert_param error line source number
  * @retval None
  */
void assert_failed(uint8_t *file, uint32_t line)
{ 
  /* USER CODE BEGIN 6 */
  /* User can add his own implementation to report the file name and line number,
     tex: printf("Wrong parameters value: file %s on line %d\r\n", file, line) */
  /* USER CODE END 6 */
}
#endif /* USE_FULL_ASSERT */

/************************ (C) COPYRIGHT STMicroelectronics *****END OF FILE****/
