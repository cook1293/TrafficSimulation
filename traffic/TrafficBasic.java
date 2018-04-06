package traffic;

/**
	信号交差点シミュレーションベーシック版
	@author cook1293
*/

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.util.Random;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.Timer;

public class TrafficBasic extends JFrame implements ActionListener{
	/* 描画用パラメータ */
	static double frameWidth   = 720+40;	//4.0*180マス + 5.0 + 45.0
	static double frameHeight  = 720+50;	//4.0*180マス + 20.0 + 20.0
	final static double topMargin	=  5.0;			//上余白
	final static double bottomMargin =  45.0;	//下余白
	final static double sideMargin   =  20.0;	//左右余白
	final static Color bgcolor = Color.white;
	final static Color fgcolor = new Color(100,0,20);
	final static BasicStroke normalStroke	 = new BasicStroke(1.0f);
	final static BasicStroke thinStroke	 = new BasicStroke(0.1f);

	/* フィールドサイズ */
	static double height,width;

	/* コンストラクタ */
	TrafficBasic(){
		setTitle("信号交差点シミュレーション");
	}

	//エージェントクラス
	public class Agent{
		/* エージェント数 */
		int nAgent = 300;
		/* エージェントx座標*/
		int [] agentX = new int [nAgent];
		/* エージェントy座標 */
		int [] agentY = new int [nAgent];
		/* エージェントx座標新規用 */
		int [] agentXNew = new int [nAgent];
		/* エージェントy座標新規用*/
		int [] agentYNew = new int [nAgent];

		/*エージェントのナンバー*/
		int numAgent = 0;
		/*エージェントの速度状態　0：停止　1:低速　2：高速*/
		double[] agentSpeed = new double[nAgent];
		/* エージェントが存在しているかどうか （falseは未発生・通過済み）*/
		Boolean [] agentLive = new Boolean [nAgent];
		/* エージェントがログを残したかどうか	（false:未記録 true:記録済）*/
		Boolean [] flgStop = new Boolean [nAgent];

		/*エージェントが発生するフレーム*/
		int bornFrame = 0;
		/*エージェントが発生するフレームの平均*/
		int bornInterval;

		/*エージェントの進行方向 0:左   1:右   2:上  3:下*/
		int [] direction = new int [nAgent];
		/*エージェントの交差点進入時の進行方向 0：直進　1：左折　2：右折*/
		int [] trafDire = new int [nAgent];

		/*記録カウンタ*/
		int cars = 0;

		//コンストラクタ
		Agent(int bi,int dir){
			initAgents(dir);		//エージェント初期設定
			bornInterval = bi;		//発生間隔の設定
		}
		/**エージェントの初期設定
		 */
		void initAgents(int dir){
			//エージェントを未誕生状態に
			for(int i=0;i<nAgent;i++){
				agentLive[i] = false;
			}
			//エージェントを初期は高速状態に
			for(int i=0;i<nAgent;i++){
				agentSpeed[i] = 2.0;
			}
			//ログは残していない状態に
			for(int i=0;i<nAgent;i++){
				flgStop[i] = false;
			}
			//進行方向の設定
			for(int i=0;i<nAgent;i++){
				direction[i] = dir;
			}

		}
		/** エージェントの発生
		*/
		void bornAgents(int dir){
			int x = 0;
			int y = 0;
			double trafDirRand = Math.random();

			//進行方向によって発生場所が変わる
			if(dir == 0){			//左移動
				x = nCellX-1;
				y = passY[0];
			} else if(dir == 1){	//右移動
				x = 1;
				y = passY[1];
			} else if(dir == 2){	//上移動
				x = passX[0];
				y = nCellY-1;
			} else {				//下移動
				x = passX[1];
				y = 1;
			}
			//乱数で交差点進入時の進行方向を決定
			if(trafDirRand <= turnLeft[dir]){
				trafDire[numAgent] = 1;	//左折
			} else if(trafDirRand >= 1-turnRight[dir]){
				trafDire[numAgent] = 2;	//右折
			} else {
				trafDire[numAgent] = 0;	//直進
			}

			agentX[numAgent] = x;
			agentY[numAgent] = y;
			agentXNew[numAgent] = x;
			agentYNew[numAgent] = y;
			agentLive[numAgent] = true;	//エージェント存在状態に
			cell[y][x] = 1;				//セルの状態
			numAgent++;					//エージェントナンバー更新
		}
	}

	/* ２次元セル */
	final static int nCellX = 180;
	final static int nCellY = 180;
	/* セルの状態 　0:何もない　1:エージェント　2:停止ブロック */
	static int [][] cell = new int [nCellY][nCellX];
	/* セルの描画サイズ */
	static double dx,dy;	//4.0

	/*通行基本位置*/
	static int[] passX = {nCellX/2-1,nCellX/2+1};	//上、下移動用
	static int[] passY = {nCellY/2+1,nCellY/2-1};	//左、右移動用
	/*交差点停止位置 0:左   1:右   2:上  3:下*/
	static int[] stopX = {passX[0]+4,passX[0]-2,passX[0],passX[1]};
	static int[] stopY = {passY[0],passY[1],passY[0]+2,passY[0]-4};
	/*交差点右左折停止位置 0:左   1:右   2:上  3:下*/
	static int[] stopTrafX = {passX[0]+1,passX[0]+1,passX[0],passX[1]};
	static int[] stopTrafY = {passY[0],passY[1],passY[0]-1,passY[0]-1};

	/*右折車がいるかどうか 0:左   1:右   2:上  3:下*/
	static int[] rightPre = {0, 0, 0, 0};


	/*交差点各進行時間フレーム*/
	static int frameHori = 120;		//横移動の青時間
	static int frameVert = 80;		//縦移動の青時間
	static int frameStop = 10;		//両方向赤の時間
	static int frameYellow = 10;		//黄色の時間
	static int stopYellow = frameYellow;	//黄色で止まるかどうか
											//黄色で止まる：frameYellow, 黄色で止まらない：0
	static int frameTurn = frameStop*2+frameHori+frameVert+frameYellow*2;
	//1サイクルにかかる時間

	/*エージェント発生間隔*/
	static int bornHoriL = 18;		//左移動発生間隔
	static int bornHoriR = 18;		//右移動発生間隔
	static int bornVertA = 28;		//上移動発生間隔
	static int bornVertB = 28;		//下移動発生間隔

	/*交差点進行方向の割合(0.0~1.0)(0:左   1:右   2:上  3:下)*/
	static double turnLeft[] = {0.2, 0.2, 0.33, 0.33};
	static double turnRight[] = {0.2, 0.2, 0.33, 0.33};


	/*左端の座標*/
	static int leftX = 5;
	/*上端の座標*/
	static int aboveY = 5;
	/*右端の座標*/
	static int rightX = nCellX-5;
	/*左端の座標*/
	static int belowY = nCellY-5;

	/* コマ送りの間隔 */
	static int delay = 100;

	/*フレーム数*/
	static int frame = 0;

	/*ターン数*/
	static int turn = 1;

	/*乱数生成*/
	static Random rand = new Random();

	/*オブジェクトの生成*/
	Timer timer;
	AnimationPane animationPane;
	Agent horiL = new Agent(bornHoriL,0);	//引数は発生間隔と方向 0:右から左へ
	Agent horiR = new Agent(bornHoriR,1);	//引数は発生間隔と方向 1:左から右へ
	Agent vertA = new Agent(bornVertA,2);	//引数は発生間隔と方向 2:下から上へ
	Agent vertB = new Agent(bornVertB,3);	//引数は発生間隔と方向 3:上から下へ


	/** 次のステップの計算
	*@param g2 描画用グラフィックス
	*/
	void goNextStep(Graphics2D g2,Agent car){
		int i,x,y;
		double px,py;
		int aheadFlg;			//進行可能フラグ(前方の空きマス数によって進めるマス数)
								//-1は右左折時
		int rightFlg;			//右折可能フラグ

		double startUp = 0.5;	//発進までの貯め
		double speedUp = 0.25;	//加速までの貯め

		//移動の決定
		for(i=0;i<car.nAgent;i++){
			//エージェントが存在していれば
			if(car.agentLive[i]){
				x = car.agentX[i];
				y = car.agentY[i];
				aheadFlg = 0;

				//左方向移動エージェント
				if(car.direction[i] == 0){
					//左端の座標までを範囲内とする
					if(x > leftX){
						if(car.trafDire[i] == 0){	//直進車用
							if(cell[y][x-1]<=0 && cell[y][x-2]<=0){
								if(cell[y][x-3]<=0 && cell[y][x-4]<=0 && cell[y][x-5]<=0){
									aheadFlg = 2;	//1~5マス先が空いていれば2マス進行可
								} else {
									aheadFlg = 1;	//1,2マス先が空いていれば1マス進行可
								}
							} else {
								aheadFlg = 0;		//空いているのが1マス以下なら進行不可
							}
						} else {	//右左折車用
							//左折の処理
							if(car.trafDire[i]==1 && x==stopTrafX[car.direction[i]]+2 && y==stopTrafY[car.direction[i]]){
								car.agentXNew[i]--;
								car.agentYNew[i]++;
								car.direction[i] = 3;
								aheadFlg = -1;
							}
							//右折の処理
							else if(car.trafDire[i]==2){
								if(x==stopTrafX[car.direction[i]]+2 && y==stopTrafY[car.direction[i]]){
									rightFlg = 1;
									//右折可能かどうか判断
									for(int j=(stopTrafX[car.direction[i]]+2);j>=(stopTrafX[car.direction[i]]-20);j--){
										if(cell[y-2][j] == 1){
											rightFlg = 0;
											break;
										}
									}
									//赤になったら強制的に右折
									if(frame == frameStop+frameHori+frameYellow){
										rightFlg = 1;
									}
									//対向車も右折車なら、強制的に右折
									if(rightPre[1] == 1){
										rightFlg = 1;
									}
									if(cell[y-1][stopTrafX[car.direction[i]]-1]==1){
										rightFlg = 1;
									}
									//右折
									if(rightFlg == 1){
										car.agentXNew[i]--;
										car.agentYNew[i]--;
										aheadFlg = -1;
										rightPre[car.direction[i]] = 0;
									} else {
										rightPre[car.direction[i]] = 1;
									}
								} else if(x==stopTrafX[car.direction[i]]+1 && y==stopTrafY[car.direction[i]]-1){
									car.agentXNew[i]--;
									car.agentYNew[i]--;
									aheadFlg = -1;
								} else if(x==stopTrafX[car.direction[i]] && y==stopTrafY[car.direction[i]]-2){
									car.agentXNew[i]--;
									car.agentYNew[i]--;
									car.direction[i] = 2;
									aheadFlg = -1;
									car.agentSpeed[i] = 1.0;
								}
							}	//右左折車交差点以外での動作
							if(aheadFlg != -1){
								if(cell[y][x-1]==0 && cell[y][x-2]==0){
									if(cell[y][x-3]==0 && cell[y][x-4]==0 && cell[y][x-5]==0){
										aheadFlg = 2;	//1~5マス先が空いていれば2マス進行可
									} else {
										aheadFlg = 1;	//1,2マス先が空いていれば1マス進行可
									}
								} else {
									aheadFlg = 0;		//空いているのが1マス以下なら進行不可
								}
							}
						}
					}

					//急加速できない直進移動
					if(car.agentSpeed[i] < 1.0){		//元々停止状態
						if(aheadFlg == 0){				//進行不可ならそのまま停止
							car.agentSpeed[i] = 0.0;
						} else {						//1マス進行可or2マス進行可なら、低速状態へためる
							car.agentSpeed[i] += startUp;
						}
					}
					else if(car.agentSpeed[i] < 2.0){	//元々低速状態
						if(aheadFlg == 0){				//進行不可なら停止状態に
							car.agentSpeed[i] = 0.0;
						} else if(aheadFlg == 1){		//1マス進行可なら1マス移動
							car.agentSpeed[i] = 1.0;
							car.agentXNew[i]--;
						} else if(aheadFlg == 2){		//2マス進行可なら、1マス移動して高速状態へためる
							car.agentXNew[i]--;
							car.agentSpeed[i] += speedUp;
						}
					} else {							//元々高速状態
						if(aheadFlg == 0){				//進行不可なら停止状態に
							car.agentSpeed[i] = 0.0;
						} else if(aheadFlg == 1){		//1マス進行可なら、低速状態にして1マス移動
							car.agentSpeed[i] = 1.0;
							car.agentXNew[i]--;
						} else if(aheadFlg == 2){		//2マス進行可なら
							car.agentXNew[i] -= 2;		//2マス移動
						}
					}


				//右方向移動エージェント
				} else if(car.direction[i] == 1){
					//右端の座標までを範囲内とする
					if(x < rightX){
						if(car.trafDire[i] == 0){
							if(cell[y][x+1]<=0 && cell[y][x+2]<=0){
								if(cell[y][x+3]<=0 && cell[y][x+4]<=0 && cell[y][x+5]<=0){
									aheadFlg = 2;	//1~5マス先が空いていれば2マス進行可
								} else {
									aheadFlg = 1;	//1,2マス先が空いていれば1マス進行可
								}
							} else {
								aheadFlg = 0;		//空いているのが1マス以下なら進行不可
							}
						} else {
							//右左折車用
							//左折の処理
							if(car.trafDire[i]==1 && x==stopTrafX[car.direction[i]]-2 && y==stopTrafY[car.direction[i]]){
								car.agentXNew[i]++;
								car.agentYNew[i]--;
								car.direction[i] = 2;
								aheadFlg = -1;
							}
							//右折の処理
							else if(car.trafDire[i]==2){
								if(x==stopTrafX[car.direction[i]]-2 && y==stopTrafY[car.direction[i]]){
									rightFlg = 1;
									//右折可能かどうか判断
									for(int j=(stopTrafX[car.direction[i]]-2);j<=(stopTrafX[car.direction[i]]+20);j++){
										if(cell[y+2][j] == 1){
											rightFlg = 0;
											break;
										}
									}
									//赤になったら強制的に右折
									if(frame == frameStop+frameHori+frameYellow){
										rightFlg = 1;
									}
									//対向車も右折車なら、強制的に右折
									if(rightPre[0] == 1){
										rightFlg = 1;
									}
									if(cell[y+1][stopTrafX[car.direction[i]]+1]==1){
										rightFlg = 1;
									}
									//右折
									if(rightFlg == 1){
										car.agentXNew[i]++;
										car.agentYNew[i]++;
										aheadFlg = -1;
										rightPre[car.direction[i]] = 0;
									} else {
										rightPre[car.direction[i]] = 1;
									}
								} else if(x==stopTrafX[car.direction[i]]-1 && y==stopTrafY[car.direction[i]]+1){
									car.agentXNew[i]++;
									car.agentYNew[i]++;
									aheadFlg = -1;
								} else if(x==stopTrafX[car.direction[i]] && y==stopTrafY[car.direction[i]]+2){
									car.agentXNew[i]++;
									car.agentYNew[i]++;
									car.direction[i] = 3;
									aheadFlg = -1;
									car.agentSpeed[i] = 1.0;
								}
							}	//右左折車交差点以外での動作
							if(aheadFlg != -1){
								if(cell[y][x+1]==0 && cell[y][x+2]==0){
									if(cell[y][x+3]==0 && cell[y][x+4]==0 && cell[y][x+5]==0){
										aheadFlg = 2;	//1~5マス先が空いていれば2マス進行可
									} else {
										aheadFlg = 1;	//1,2マス先が空いていれば1マス進行可
									}
								} else {
									aheadFlg = 0;		//空いているのが1マス以下なら進行不可
								}
							}
						}
					}

					//急加速できない直進移動
					if(car.agentSpeed[i] < 1.0){		//元々停止状態
						if(aheadFlg == 0){				//進行不可ならそのまま停止
							car.agentSpeed[i] = 0.0;
						} else {						//1マス進行可or2マス進行可なら、低速状態へためる
							car.agentSpeed[i] += startUp;
						}
					}
					else if(car.agentSpeed[i] < 2.0){	//元々低速状態
						if(aheadFlg == 0){				//進行不可なら停止状態に
							car.agentSpeed[i] = 0.0;
						} else if(aheadFlg == 1){		//1マス進行可なら1マス移動
							car.agentSpeed[i] = 1.0;
							car.agentXNew[i]++;
						} else if(aheadFlg == 2){		//2マス進行可なら、1マス移動して高速状態へためる
							car.agentXNew[i]++;
							car.agentSpeed[i] += speedUp;
						}
					} else {							//元々高速状態
						if(aheadFlg == 0){				//進行不可なら停止状態に
							car.agentSpeed[i] = 0.0;
						} else if(aheadFlg == 1){		//1マス進行可なら、低速状態にして1マス移動
							car.agentSpeed[i] = 1.0;
							car.agentXNew[i]++;
						} else if(aheadFlg == 2){		//2マス進行可なら
							car.agentXNew[i] += 2;		//2マス移動
						}
					}

				//上方向移動エージェント
				} else if(car.direction[i] == 2){
					//上端の座標までを範囲内とする
					if(y > aboveY){
						if(car.trafDire[i] == 0){
							if(cell[y-1][x]<=0 && cell[y-2][x]<=0){
								if(cell[y-3][x]<=0 && cell[y-4][x]<=0 && cell[y-5][x]<=0){
									aheadFlg = 2;	//1~5マス先が空いていれば2マス進行可
								} else {
									aheadFlg = 1;	//1,2マス先が空いていれば1マス進行可
								}
							} else {
								aheadFlg = 0;		//空いているのが1マス以下なら進行不可
							}
						} else {
							//右左折車用
							//左折の処理
							if(car.trafDire[i]==1 && x==stopTrafX[car.direction[i]] && y==stopTrafY[car.direction[i]]+2){
								car.agentXNew[i]--;
								car.agentYNew[i]--;
								car.direction[i] = 0;
								aheadFlg = -1;
							}
							//右折の処理
							else if(car.trafDire[i]==2){
								if(x==stopTrafX[car.direction[i]] && y==stopTrafY[car.direction[i]]+2){
									rightFlg = 1;
									//右折可能かどうか判断
									for(int j=(stopTrafY[car.direction[i]]+2);j>=(stopTrafY[car.direction[i]]-20);j--){
										if(cell[j][x+2] == 1){
											rightFlg = 0;
											break;
										}
									}
									//赤になったら強制的に右折
									if(frame == 0){
										rightFlg = 1;
									}
									//対向車も右折車なら、強制的に右折
									if(rightPre[3] == 1){
										rightFlg = 1;
									}
									if(cell[stopTrafY[car.direction[i]]-1][x+1]==1){
										rightFlg = 1;
									}
									//右折
									if(rightFlg == 1){
										car.agentXNew[i]++;
										car.agentYNew[i]--;
										aheadFlg = -1;
										rightPre[car.direction[i]] = 0;
									} else {
										rightPre[car.direction[i]] = 1;
									}
								} else if(x==stopTrafX[car.direction[i]]+1 && y==stopTrafY[car.direction[i]]+1){
									car.agentXNew[i]++;
									car.agentYNew[i]--;
									aheadFlg = -1;
								} else if(x==stopTrafX[car.direction[i]]+2 && y==stopTrafY[car.direction[i]]){
									car.agentXNew[i]++;
									car.agentYNew[i]--;
									car.direction[i] = 1;
									aheadFlg = -1;
									car.agentSpeed[i] = 1.0;
								}
							}	//右左折車交差点以外での動作
							if(aheadFlg != -1){
								if(cell[y-1][x]==0 && cell[y-2][x]==0){
									if(cell[y-3][x]==0 && cell[y-4][x]==0 && cell[y-5][x]==0){
										aheadFlg = 2;	//1~5マス先が空いていれば2マス進行可
									} else {
										aheadFlg = 1;	//1,2マス先が空いていれば1マス進行可
									}
								} else {
									aheadFlg = 0;		//空いているのが1マス以下なら進行不可
								}
							}
						}
					}

					//急加速できない直進移動
					if(car.agentSpeed[i] < 1.0){		//元々停止状態
						if(aheadFlg == 0){				//進行不可ならそのまま停止
							car.agentSpeed[i] = 0.0;
						} else {						//1マス進行可or2マス進行可なら、低速状態へためる
							car.agentSpeed[i] += startUp;
						}
					}
					else if(car.agentSpeed[i] < 2.0){	//元々低速状態
						if(aheadFlg == 0){				//進行不可なら停止状態に
							car.agentSpeed[i] = 0.0;
						} else if(aheadFlg == 1){		//1マス進行可なら1マス移動
							car.agentSpeed[i] = 1.0;
							car.agentYNew[i]--;
						} else if(aheadFlg == 2){		//2マス進行可なら、1マス移動して高速状態へためる
							car.agentYNew[i]--;
							car.agentSpeed[i] += speedUp;
						}
					} else {							//元々高速状態
						if(aheadFlg == 0){				//進行不可なら停止状態に
							car.agentSpeed[i] = 0.0;
						} else if(aheadFlg == 1){		//1マス進行可なら、低速状態にして1マス移動
							car.agentSpeed[i] = 1.0;
							car.agentYNew[i]--;
						} else if(aheadFlg == 2){		//2マス進行可なら
							car.agentYNew[i] -= 2;		//2マス移動
						}
					}

				//下方向移動エージェント
				} else {
					//下端の座標までを範囲内とする
					if(y < belowY){
						if(car.trafDire[i] == 0){
							if(cell[y+1][x]<=0 && cell[y+2][x]<=0){
								if(cell[y+3][x]<=0 && cell[y+4][x]<=0 && cell[y+5][x]<=0){
									aheadFlg = 2;	//1~5マス先が空いていれば2マス進行可
								} else {
									aheadFlg = 1;	//1,2マス先が空いていれば1マス進行可
								}
							} else {
								aheadFlg = 0;		//空いているのが1マス以下なら進行不可
							}
						} else {
							//右左折車用
							//左折の処理
							if(car.trafDire[i]==1 && x==stopTrafX[car.direction[i]] && y==stopTrafY[car.direction[i]]-2){
								car.agentXNew[i]++;
								car.agentYNew[i]++;
								car.direction[i] = 1;
								aheadFlg = -1;
							}
							//右折の処理
							else if(car.trafDire[i]==2){
								if(x==stopTrafX[car.direction[i]] && y==stopTrafY[car.direction[i]]-2){
									rightFlg = 1;
									//右折可能かどうか判断
									for(int j=(stopTrafY[car.direction[i]]-2);j<=(stopTrafY[car.direction[i]]+20);j++){
										if(cell[j][x-2] == 1){
											rightFlg = 0;
											break;
										}
									}
									//赤になったら強制的に右折
									if(frame == 0){
										rightFlg = 1;
									}
									//対向車も右折車なら、強制的に右折
									if(rightPre[2] == 1){
										rightFlg = 1;
									}
									if(cell[stopTrafY[car.direction[i]]+1][x-1]==1){
										rightFlg = 1;
									}
									//右折
									if(rightFlg == 1){
										car.agentXNew[i]--;
										car.agentYNew[i]++;
										aheadFlg = -1;
										rightPre[car.direction[i]] = 0;
									} else {
										rightPre[car.direction[i]] = 1;
									}
								} else if(x==stopTrafX[car.direction[i]]-1 && y==stopTrafY[car.direction[i]]-1){
									car.agentXNew[i]--;
									car.agentYNew[i]++;
									aheadFlg = -1;
								} else if(x==stopTrafX[car.direction[i]]-2 && y==stopTrafY[car.direction[i]]){
									car.agentXNew[i]--;
									car.agentYNew[i]++;
									car.direction[i] = 0;
									aheadFlg = -1;
									car.agentSpeed[i] = 1.0;
								}
							}	//右左折車交差点以外での動作
							if(aheadFlg != -1){
								if(cell[y+1][x]==0 && cell[y+2][x]==0){
									if(cell[y+3][x]==0 && cell[y+4][x]==0 && cell[y+5][x]==0){
										aheadFlg = 2;	//1~5マス先が空いていれば2マス進行可
									} else {
										aheadFlg = 1;	//1,2マス先が空いていれば1マス進行可
									}
								} else {
									aheadFlg = 0;		//空いているのが1マス以下なら進行不可
								}
							}
						}
					}

					//急加速できない直進移動
					if(car.agentSpeed[i] < 1.0){		//元々停止状態
						if(aheadFlg == 0){				//進行不可ならそのまま停止
							car.agentSpeed[i] = 0.0;
						} else {						//1マス進行可or2マス進行可なら、低速状態へためる
							car.agentSpeed[i] += startUp;
						}
					}
					else if(car.agentSpeed[i] < 2.0){	//元々低速状態
						if(aheadFlg == 0){				//進行不可なら停止状態に
							car.agentSpeed[i] = 0.0;
						} else if(aheadFlg == 1){		//1マス進行可なら1マス移動
							car.agentSpeed[i] = 1.0;
							car.agentYNew[i]++;
						} else if(aheadFlg == 2){		//2マス進行可なら、1マス移動して高速状態へためる
							car.agentYNew[i]++;
							car.agentSpeed[i] += speedUp;
						}
					} else {							//元々高速状態
						if(aheadFlg == 0){				//進行不可なら停止状態に
							car.agentSpeed[i] = 0.0;
						} else if(aheadFlg == 1){		//1マス進行可なら、低速状態にして1マス移動
							car.agentSpeed[i] = 1.0;
							car.agentYNew[i]++;
						} else if(aheadFlg == 2){		//2マス進行可なら
							car.agentYNew[i] += 2;		//2マス移動
						}
					}

				}
			}
		}

		//移動の描画
		for(i=0;i<car.nAgent;i++){
			if(car.agentLive[i]){
				//描画
				cell[car.agentY[i]][car.agentX[i]] = 0;
				cell[car.agentYNew[i]][car.agentXNew[i]] = 1;
				g2.setPaint(Color.white);
				px = sideMargin + car.agentX[i] * dx;
				py = topMargin  + car.agentY[i] * dy;
				g2.fill(new Rectangle2D.Double(px,py,dy,dx));

				if(car.trafDire[i] == 0){
					g2.setPaint(Color.blue);	//直進車
				} else if(car.trafDire[i] == 1){
					g2.setPaint(Color.cyan);	//左折車
				} else {
					g2.setPaint(Color.magenta);	//右折車
				}

				px = sideMargin + car.agentXNew[i] * dx;
				py = topMargin  + car.agentYNew[i] * dy;
				g2.fill(new Rectangle2D.Double(px,py,dy,dx));

				//端に到達したエージェントを排除する
				if((car.direction[i]==0&&car.agentXNew[i]<=leftX)
				 ||(car.direction[i]==1&&car.agentXNew[i]>=rightX)
				 ||(car.direction[i]==2&&car.agentYNew[i]<=aboveY)
				 ||(car.direction[i]==3&&car.agentYNew[i]>=belowY)){
					g2.setPaint(Color.white);
					px = sideMargin + car.agentXNew[i] * dx;
					py = topMargin  + car.agentYNew[i] * dy;
					g2.fill(new Rectangle2D.Double(px,py,dy,dx));
					car.agentLive[i] = false;
					//agentSpeed[i] = 0.0;
					cell[car.agentYNew[i]][car.agentXNew[i]] = 0;
				}
				//座標の更新
				car.agentX[i] = car.agentXNew[i];
				car.agentY[i] = car.agentYNew[i];
			}
		}
	}

	/**
	 * 建造物の描画関数
	 */
	void construction(Graphics2D g2){
		//道路の描画
		int aboveSide = passY[0]-4;
		int belowSide = passY[0]+2;
		int rightSide = passX[0]+4;
		int leftSide  = passX[0]-2;
		g2.setPaint(fgcolor);
		for(int x=0;x<leftSide;x++){
			g2.fill(new Rectangle2D.Double(sideMargin + x * dx,
					topMargin  + belowSide * dy, dx, dy));
		}
		for(int x=rightSide;x<nCellX;x++){
			g2.fill(new Rectangle2D.Double(sideMargin + x * dx,
					topMargin  + belowSide * dy, dx, dy));
		}
		for(int x=0;x<leftSide+1;x++){
			g2.fill(new Rectangle2D.Double(sideMargin + x * dx,
					topMargin  + aboveSide * dy, dx, dy));
		}
		for(int x=rightSide;x<nCellX;x++){
			g2.fill(new Rectangle2D.Double(sideMargin + x * dx,
					topMargin  + aboveSide * dy, dx, dy));
		}
		for(int y=0;y<aboveSide;y++){
			g2.fill(new Rectangle2D.Double(sideMargin + (leftSide) * dx,
					topMargin  + y * dy, dx, dy));
		}
		for(int y=belowSide;y<nCellY;y++){
			g2.fill(new Rectangle2D.Double(sideMargin + (leftSide) * dx,
					topMargin  + y * dy, dx, dy));
		}
		for(int y=0;y<aboveSide;y++){
			g2.fill(new Rectangle2D.Double(sideMargin + rightSide * dx,
					topMargin  + y * dy, dx, dy));
		}
		for(int y=belowSide;y<nCellY;y++){
			g2.fill(new Rectangle2D.Double(sideMargin + rightSide * dx,
					topMargin  + y * dy, dx, dy));
		}

		//主信号(左右方向)の描画
		if(frame<=frameStop || frame >= frameStop+frameHori+frameYellow){
			g2.setPaint(Color.red);		//赤
		} else {
			g2.setPaint(Color.gray);
		}
		g2.fill(new Rectangle2D.Double(sideMargin + (leftSide-2) * dx,
				topMargin  + (belowSide+2) * dy, dx, dy));
		if(frame>=frameStop+frameHori && frame<frameStop+frameHori+frameYellow){
			g2.setPaint(Color.yellow);	//黄
		} else {
			g2.setPaint(Color.gray);
		}
		g2.fill(new Rectangle2D.Double(sideMargin + (leftSide-2) * dx,
				topMargin  + (belowSide+3) * dy, dx, dy));
		if(frame>frameStop && frame<frameStop+frameHori){
			g2.setPaint(Color.green);	//緑
		} else {
			g2.setPaint(Color.gray);
		}
		g2.fill(new Rectangle2D.Double(sideMargin + (leftSide-2) * dx,
				topMargin  + (belowSide+4) * dy, dx, dy));

		//補助信号(上下方向)の描画
		if(frame<=frameStop*2+frameHori+frameYellow || frame >= frameStop*2+frameHori+frameVert+frameYellow*2){
			g2.setPaint(Color.red);		//赤
		} else {
			g2.setPaint(Color.gray);
		}
		g2.fill(new Rectangle2D.Double(sideMargin + (leftSide-2) * dx,
				topMargin  + (aboveSide-2) * dy, dx, dy));
		if(frame>=frameStop*2+frameHori+frameVert+frameYellow && frame<frameStop*2+frameHori+frameVert+frameYellow*2){
			g2.setPaint(Color.yellow);	//黄
		} else {
			g2.setPaint(Color.gray);
		}
		g2.fill(new Rectangle2D.Double(sideMargin + (leftSide-3) * dx,
				topMargin  + (aboveSide-2) * dy, dx, dy));
		if(frame>frameStop*2+frameHori+frameYellow && frame<frameStop*2+frameHori+frameVert+frameYellow){
			g2.setPaint(Color.green);	//緑
		} else {
			g2.setPaint(Color.gray);
		}
		g2.fill(new Rectangle2D.Double(sideMargin + (leftSide-4) * dx,
				topMargin  + (aboveSide-2) * dy, dx, dy));

	}

	/** 初期値の設定
	*@param container フレーム
	*/
	void buildUI(Container container){
		timer = new Timer(delay, this);
		timer.setInitialDelay(0);
		timer.setCoalesce(true);
		animationPane = new AnimationPane();
		container.add(animationPane, BorderLayout.CENTER);
		Dimension d = getSize();
		frameWidth  = d.width;
		frameHeight = d.height;
		width =  frameWidth - 2 * sideMargin;
		height = frameHeight - (topMargin + bottomMargin);
		dx = width / nCellX;
		dy = height / nCellY;
	}

	/**
	* タイマーにより一定間隔で呼び出し
	*/
	public void actionPerformed(ActionEvent e){
		animationPane.repaint();	//再描画
	}

	/**
	* タイマーにより一定間隔で描画される
	*/
	class AnimationPane extends JPanel{

		// Draw the current frame of animation.
		public void paintComponent(Graphics g){
			Graphics2D g2 = (Graphics2D) g;
			g2.setStroke(thinStroke);
			g2.setPaint(fgcolor);
			int x,y;

			// 枠の矩形を描画する
			g2.draw(new Rectangle2D.Double(sideMargin,topMargin,width,height));

			//停止位置設定・描画
			//左移動
			if(frame<=frameStop || frame >= frameStop+frameHori+frameYellow-stopYellow){
				cell[stopY[0]][stopX[0]] = 2;	//停止
				if(frame == frameStop+frameHori+frameYellow){
					cell[stopTrafY[0]][stopTrafX[0]] = 0;	//右左折停止無効
				}
				g2.setPaint(Color.gray);
				//渋滞ログ
				if(frame == frameStop){
					x = stopX[0];
					y = stopY[0];
					horiL.cars = 0;
					if(cell[y][x+1]==1){	//ぎりぎりで止まってしまった時のため
						x += 1;
						horiL.cars++;
					} else if(cell[y][x+2]==1){
						x += 2;
						horiL.cars++;
					}
					while(cell[y][x+2]==1){
						horiL.cars++;
						x += 2;
					}
					System.out.print(turn+"ターン　←左："+horiL.cars);
				}
			} else {
				if(cell[stopY[0]][stopX[0]] == 2){
					cell[stopY[0]][stopX[0]] = 0;		//進行
				}
				cell[stopTrafY[0]][stopTrafX[0]] = -1;	//右左折停止有効
				g2.setPaint(Color.white);
			}
			//g2.fill(new Rectangle2D.Double(sideMargin + stopX[0] * dx,	topMargin  + stopY[0] * dy, dx, dy));

			//右移動
			if(frame<=frameStop || frame >= frameStop+frameHori+frameYellow-stopYellow){
				cell[stopY[1]][stopX[1]] = 2;	//停止
				if(frame == frameStop+frameHori+frameYellow){
					cell[stopTrafY[1]][stopTrafX[1]] = 0;	//右左折停止無効
				}
				g2.setPaint(Color.gray);
				//渋滞ログ
				if(frame == frameStop){
					x = stopX[1];
					y = stopY[1];
					horiR.cars = 0;
					if(cell[y][x-1]==1){	//ぎりぎりで止まってしまった時のため
						x -= 1;
						horiR.cars++;
					} else if(cell[y][x-2]==1){
						x -= 2;
						horiR.cars++;
					}
					while(cell[y][x-2]==1){
						horiR.cars++;
						x -= 2;
					}
					System.out.println("　　→右："+horiR.cars);
				}
			} else {
				if(cell[stopY[1]][stopX[1]] == 2){
					cell[stopY[1]][stopX[1]] = 0;		//進行
				}
				cell[stopTrafY[1]][stopTrafX[1]] = -1;	//右左折停止有効
				g2.setPaint(Color.white);
			}
			//g2.fill(new Rectangle2D.Double(sideMargin + stopX[1] * dx,	topMargin  + stopY[1] * dy, dx, dy));

			//上移動
			if(frame<=frameStop*2+frameHori+frameYellow || frame >= frameStop*2+frameHori+frameVert+frameYellow*2-stopYellow){
				cell[stopY[2]][stopX[2]] = 2;
				if(frame >= frameStop*2+frameHori+frameVert+frameYellow*2){
					cell[stopTrafY[2]][stopTrafX[2]] = 0;	//右左折停止無効
				}
				g2.setPaint(Color.gray);
				//渋滞ログ
				if(frame == frameStop*2+frameHori+frameYellow){
					x = stopX[2];
					y = stopY[2];
					vertA.cars = 0;
					if(cell[y+1][x]==1){	//ぎりぎりで止まってしまった時のため
						y += 1;
						vertA.cars++;
					} else if(cell[y+2][x]==1){
						y += 2;
						vertA.cars++;
					}
					while(cell[y+2][x]==1){
						vertA.cars++;
						y += 2;
					}
					System.out.print(turn+"ターン　↑上："+vertA.cars);
				}
			} else {
				if(cell[stopY[2]][stopX[2]] == 2){
					cell[stopY[2]][stopX[2]] = 0;		//進行
				}
				cell[stopTrafY[2]][stopTrafX[2]] = -1;	//右左折停止有効
				g2.setPaint(Color.white);
			}
			//g2.fill(new Rectangle2D.Double(sideMargin + stopX[2] * dx,	topMargin  + stopY[2] * dy, dx, dy));

			//下移動
			if(frame<=frameStop*2+frameHori+frameYellow || frame >= frameStop*2+frameHori+frameVert+frameYellow*2-stopYellow){
				cell[stopY[3]][stopX[3]] = 2;
				if(frame >= frameStop*2+frameHori+frameVert+frameYellow*2){
					cell[stopTrafY[3]][stopTrafX[3]] = 0;	//右左折停止無効
				}
				g2.setPaint(Color.gray);
				//渋滞ログ
				if(frame == frameStop*2+frameHori+frameYellow){
					x = stopX[3];
					y = stopY[3];
					vertB.cars = 0;
					if(cell[y-1][x]==1){	//ぎりぎりで止まってしまった時のため
						y -= 1;
						vertB.cars++;
					} else if(cell[y-2][x]==1){
						y -= 2;
						vertB.cars++;
					}
					while(cell[y-2][x]==1){
						vertB.cars++;
						y -= 2;
					}
					System.out.println("　　↓下："+vertB.cars);
				}
			} else {
				if(cell[stopY[3]][stopX[3]] == 2){
					cell[stopY[3]][stopX[3]] = 0;		//進行
				}
				cell[stopTrafY[3]][stopTrafX[3]] = -1;	//右左折停止有効
				g2.setPaint(Color.white);
			}
			//g2.fill(new Rectangle2D.Double(sideMargin + stopX[3] * dx,	topMargin  + stopY[3] * dy, dx, dy));


			//エージェントの発生(発生間隔は指数分布に従わせている)
			//左移動
			if(frame == (horiL.bornFrame%frameTurn)){
				if(horiL.numAgent < horiL.nAgent){
					horiL.bornAgents(0);
					horiL.bornFrame += (int)((-1*horiL.bornInterval*Math.log(1.0-Math.random()))+1);
				}
			}
			//右移動
			if(frame == (horiR.bornFrame%frameTurn)){
				if(horiR.numAgent < horiR.nAgent){
					horiR.bornAgents(1);
					horiR.bornFrame += (int)((-1*horiR.bornInterval*Math.log(1.0-Math.random()))+1);
				}
			}
			//上移動
			if(frame == (vertA.bornFrame%frameTurn)){
				if(vertA.numAgent < vertA.nAgent){
					vertA.bornAgents(2);
					vertA.bornFrame += (int)((-1*vertA.bornInterval*Math.log(1.0-Math.random()))+1);
				}
			}
			//下移動
			if(frame == (vertB.bornFrame%frameTurn)){
				if(vertB.numAgent < vertB.nAgent){
					vertB.bornAgents(3);
					vertB.bornFrame += (int)((-1*vertB.bornInterval*Math.log(1.0-Math.random()))+1);
				}
			}


			//エージェントの更新
			goNextStep(g2,horiL);	//左移動
			goNextStep(g2,horiR);	//右移動
			goNextStep(g2,vertA);	//上移動
			goNextStep(g2,vertB);	//下移動

			//建造物の描画(道路・信号)
			construction(g2);

			//線の描画
			g2.setPaint(Color.lightGray);
			for(int i=1;i<nCellX;i++){
				g2.draw(new Line2D.Double(sideMargin+dx*i, topMargin, sideMargin+dx*i, topMargin+height));
			}
			for(int i=1;i<nCellY;i++){
				g2.draw(new Line2D.Double(sideMargin, topMargin+dy*i, sideMargin+width, topMargin+dy*i));
			}


			/*
			g2.setPaint(Color.gray);
			for(int i=0;i<4;i++){
				g2.fill(new Rectangle2D.Double(sideMargin + stopTrafX[i] * dx,	topMargin  + stopTrafY[i] * dy, dx, dy));
			}
			*/

			//情報の表示ラベル
			g2.setPaint(Color.white);
			g2.fill(new Rectangle2D.Double(480,500,120,30));
			g2.fill(new Rectangle2D.Double(630,500,100,30));
			/*
			g2.fill(new Rectangle2D.Double(370,550,50,30));
			g2.fill(new Rectangle2D.Double(450,550,120,30));
			g2.fill(new Rectangle2D.Double(600,550,120,30));
			g2.fill(new Rectangle2D.Double(370,600,50,30));
			g2.fill(new Rectangle2D.Double(450,600,120,30));
			g2.fill(new Rectangle2D.Double(600,600,120,30));
			*/
			g2.setPaint(Color.black);
			g2.setFont(new Font("SansSerif",Font.BOLD,24));
			g2.drawString(Integer.toString(frame)+" frame",480,525);	//フレーム数
			g2.drawString(Integer.toString(turn)+" turn",630,525);		//ターン数
			/*
			g2.drawString("横",370,575);
			g2.drawString(Integer.toString(frameHori)+" frame",450,575);
			g2.drawString(Integer.toString(horiL.cars)+" cars",600,575);
			g2.drawString("縦",370,625);
			g2.drawString(Integer.toString(frameVert)+" frame",450,625);
			g2.drawString(Integer.toString(vertA.cars)+" cars",600,625);
			*/

			//フレームの更新
			frame++;
			if(frame == frameTurn){
				frame = 0;
				turn++;		//ターンの更新
			}

		}
	}

	/**
	 * main関数
	 */
	public static void main(String argv[]) {

		/* フレームの宣言 */
		final TrafficBasic controller = new TrafficBasic();

		/* フレームサイズ */
		controller.setSize(new Dimension((int)frameWidth,
			(int)frameHeight));
		controller.setBackground(bgcolor);
		controller.setForeground(fgcolor);

		/* パラメータ設定 */
		controller.buildUI(controller.getContentPane());

		/* フレーム可視化 */
		controller.setVisible(true);

		/* タイマースタート */
		controller.timer.start();

		/* パラメーターの表示 */
		System.out.println("発生間隔	左："+bornHoriL+"frame/台	右："+bornHoriR+"frame/台");
		System.out.println("発生間隔	上："+bornVertA+"frame/台	下："+bornVertB+"frame/台");
		System.out.println("青時間横	"+frameHori+"frame　　"+frameStop+"　~　"+(frameStop+frameHori));
		System.out.println("青時間縦	"+frameVert+"frame　　"+(frameStop*2+frameHori+frameYellow)+"　~　"+(frameStop*2+frameHori+frameVert+frameYellow));
		System.out.print("黄信号：");
		if(stopYellow==0){
			System.out.println("進行\n");
		} else {
			System.out.println("停止\n");
		}

		/* 閉じる処理 */
		controller.setDefaultCloseOperation(EXIT_ON_CLOSE);
	}
}







